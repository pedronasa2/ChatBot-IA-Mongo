package br.com.painel;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.painel.agenda.service.AgendamentoService;
import br.com.painel.candidatos.service.CandidatoService;
import br.com.painel.configuracoes.model.vo.DadosRascunhoBot;
import br.com.painel.configuracoes.repository.ConfiguracaoBotRepository;
import br.com.painel.configuracoes.service.ConfiguracaoBotService;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.model.security.Credenciais;
import br.com.painel.empresas.model.vo.DadosEmpresa;
import br.com.painel.empresas.model.vo.PermissoesModulos;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.empresas.service.EmpresaService;
import br.com.painel.empresas.service.PermissoesModulosService;
import br.com.painel.superusuario.RepositorioMetricasReferencia;
import br.com.painel.superusuario.model.vo.DadosPromptGlobal;
import br.com.painel.superusuario.model.vo.EventoMensagem;
import br.com.painel.superusuario.model.vo.VolumePorMinuto;
import br.com.painel.superusuario.repository.MetricasAtendimentoRepository;
import br.com.painel.superusuario.resource.IntegracaoMetricasResource;
import br.com.painel.superusuario.service.PromptGlobalService;
import br.com.painel.superusuario.service.RegistroMensagensService;

import jakarta.servlet.http.Cookie;

import org.bson.Document;
import org.junit.jupiter.api.*;
import org.mongodb.spring.session.MongoIndexedSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
class BaseIntegrationTest {
    @Autowired MockMvc mvc;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("prepararMongo")
    org.springframework.boot.ApplicationRunner prepararMongo;

    @Autowired MongoTemplate mongo;
    @Autowired PasswordEncoder encoder;
    UUID user, own, other;
    Cookie session;
    @Autowired MongoIndexedSessionRepository sessions;
    @Autowired org.mongodb.spring.session.AbstractMongoSessionConverter sessionConverter;
    @Autowired PlatformTransactionManager transactions;
    @Autowired RegistroMensagensService registroMensagens;
    @Autowired EmpresaService servicoEmpresa;
    @Autowired ConfiguracaoBotService servicoBot;
    @Autowired PermissoesModulosService servicoPermissoes;
    @Autowired PromptGlobalService servicoPrompt;
    @Autowired CandidatoService servicoCandidatos;
    @Autowired AgendamentoService servicoAgenda;
    @Autowired EmpresaRepository repositorioEmpresa;
    @Autowired ConfiguracaoBotRepository repositorioBot;

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.mongodb.uri", () -> MongoFixture.URI);
        r.add("server.servlet.session.cookie.secure", () -> false);
        r.add("painel.integracao.token-modulos", () -> "token-modulos-teste-2026");
        r.add("painel.integracao.token-metricas", () -> "credencial-exclusiva-teste-2026");
    }

    @AfterAll
    static void limparBanco(@Autowired MongoTemplate mongo) {
        mongo.getDb().drop();
    }

    @BeforeEach
    void setup() throws Exception {
        for (String c : mongo.getCollectionNames()) mongo.remove(new Query(), c);
        MongoFixture.global(mongo);
        user = UUID.randomUUID();
        own = UUID.randomUUID();
        other = UUID.randomUUID();
        MongoFixture.usuario(
                mongo, user, "owner@example.test", encoder.encode("SenhaSomenteTeste-2026"), false);
        for (UUID id : List.of(own, other)) MongoFixture.empresa(mongo, id, user);
        MongoFixture.vinculo(mongo, user, own);
        session =
                (Cookie)
                        mvc.perform(
                                        post("/api/auth/login")
                                                .with(csrf())
                                                .param("email", "owner@example.test")
                                                .param("senha", "SenhaSomenteTeste-2026"))
                                .andExpect(status().isNoContent())
                                .andReturn()
                                .getResponse()
                                .getCookie("SESSION");
        assertThat(session).isNotNull();
    }

    private String novaEmpresaJson(String email) {
        return new Document("nome", "Empresa convidada")
                .append("responsavel", "Responsável")
                .append("email", email)
                .append("moduloCandidatosHabilitado", true)
                .append("moduloAgendaHabilitado", false)
                .toJson();
    }

    private String ativacaoJson(String token, String email) {
        return new Document("token", token)
                .append("email", email)
                .append("senha", "SenhaNovaTeste2026")
                .append("confirmacaoSenha", "SenhaNovaTeste2026")
                .toJson();
    }

    @Test
    void migraVinculoLegadoSemSobrescreverConfiguracaoNova() throws Exception {
        mongo.insert(
                new Document("empresa_id", own).append("account_id", 1L).append("inbox_id", 1L),
                "integracoes_chatwoot");
        prepararMongo.run(null);
        var empresa = mongo.findOne(consultaPor("_id", own), Document.class, "empresas");
        assertThat(empresa.get("chatwoot_account_id")).isEqualTo(1L);
        assertThat(empresa.get("chatwoot_inbox_id")).isEqualTo(1L);
        mongo.updateFirst(
                consultaPor("_id", own), new Update().set("chatwoot_inbox_id", 2L), "empresas");
        prepararMongo.run(null);
        assertThat(
                        mongo.findOne(consultaPor("_id", own), Document.class, "empresas")
                                .get("chatwoot_inbox_id"))
                .isEqualTo(2L);
    }

    @Test
    void vinculoChatwootExclusivoUnicoEPersistente() throws Exception {
        String endpoint = "/api/superusuario/empresas/" + own + "/chatwoot";
        String dados = "{\"accountId\":1,\"inboxId\":1}";
        mvc.perform(get(endpoint).cookie(session)).andExpect(status().isForbidden());
        mvc.perform(
                        put(endpoint)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(dados))
                .andExpect(status().isForbidden());
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        mvc.perform(get(endpoint).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").isEmpty());
        mvc.perform(put(endpoint).cookie(session).contentType("application/json").content(dados))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put(endpoint)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"accountId\":0,\"inboxId\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put(endpoint)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(dados))
                .andExpect(status().isOk());
        mvc.perform(get(endpoint).cookie(session))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.inboxId").value(1));
        mvc.perform(
                        put("/api/superusuario/empresas/" + other + "/chatwoot")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(dados))
                .andExpect(status().isConflict());
        mvc.perform(
                        put(endpoint)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"accountId\":1,\"inboxId\":2}"))
                .andExpect(status().isOk());
        assertThat(
                        mongo.findOne(consultaPor("_id", own), Document.class, "empresas")
                                .get("chatwoot_inbox_id"))
                .isEqualTo(2L);
        assertThat(mongo.count(new Query(), "integracoes_chatwoot")).isZero();
        mvc.perform(
                        put("/api/superusuario/empresas/" + UUID.randomUUID() + "/chatwoot")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(dados))
                .andExpect(status().isNotFound());
    }

    @Test
    void conviteExclusivoDoMasterAtivaSenhaUmaVezEIsolaEmpresa() throws Exception {
        mvc.perform(
                        post("/api/superusuario/empresas")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(novaEmpresaJson("novo@example.test")))
                .andExpect(status().isForbidden());
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        var r =
                mvc.perform(
                                post("/api/superusuario/empresas")
                                        .cookie(session)
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content(novaEmpresaJson("novo@example.test")))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse();
        var convite = Document.parse(r.getContentAsString());
        String token = convite.getString("token");
        UUID empresa = UUID.fromString(convite.getString("empresaId"));
        assertThat(
                        mongo.findOne(
                                        consultaPor("email", "novo@example.test"),
                                        Document.class,
                                        "usuarios")
                                .getBoolean("ativo"))
                .isFalse();
        assertThat(
                        mongo.findOne(
                                        consultaPor("empresa_id", empresa),
                                        Document.class,
                                        "convites_empresas")
                                .toJson())
                .doesNotContain(token);
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(ativacaoJson(token, "outro@example.test")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .contentType("application/json")
                                .content(ativacaoJson(token, "novo@example.test")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(ativacaoJson(token, "novo@example.test")))
                .andExpect(status().isNoContent());
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(ativacaoJson(token, "novo@example.test")))
                .andExpect(status().isBadRequest());
        var novoCookie =
                mvc.perform(
                                post("/api/auth/login")
                                        .with(csrf())
                                        .param("email", "novo@example.test")
                                        .param("senha", "SenhaNovaTeste2026"))
                        .andExpect(status().isNoContent())
                        .andReturn()
                        .getResponse()
                        .getCookie("SESSION");
        mvc.perform(get("/api/empresas").cookie(novoCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(empresa.toString()));
        mvc.perform(get("/api/empresas/" + own).cookie(novoCookie))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/api/superusuario/empresas")
                                .cookie(novoCookie)
                                .with(csrf())
                                .contentType("application/json")
                                .content(novaEmpresaJson("nao@example.test")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/superusuario/empresas/" + empresa + "/convite")
                                .cookie(session)
                                .with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(
                        post("/api/superusuario/empresas")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(novaEmpresaJson("novo@example.test")))
                .andExpect(status().isConflict());
    }

    @Test
    void renovarConviteRevogaAnteriorEExpiradoNaoAtiva() throws Exception {
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        var r =
                mvc.perform(
                                post("/api/superusuario/empresas")
                                        .cookie(session)
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content(novaEmpresaJson("expirado@example.test")))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse();
        var c = Document.parse(r.getContentAsString());
        UUID empresa = UUID.fromString(c.getString("empresaId"));
        var novo =
                Document.parse(
                        mvc.perform(
                                        post("/api/superusuario/empresas/" + empresa + "/convite")
                                                .cookie(session)
                                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString());
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        ativacaoJson(
                                                c.getString("token"), "expirado@example.test")))
                .andExpect(status().isBadRequest());
        mongo.updateFirst(
                consultaPor("empresa_id", empresa),
                new Update().set("expira_em", Date.from(Instant.now().minusSeconds(60))),
                "convites_empresas");
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        ativacaoJson(
                                                novo.getString("token"), "expirado@example.test")))
                .andExpect(status().isBadRequest());
        assertThat(
                        mongo.findOne(
                                        consultaPor("email", "expirado@example.test"),
                                        Document.class,
                                        "usuarios")
                                .getBoolean("ativo"))
                .isFalse();
    }

    @Test
    void sessaoPermanenteNaoFicaElegivelParaLimpezaTtl() throws Exception {
        var permanente = sessions.createSession();
        permanente.setAttribute("teste", "preservado");
        sessions.save(permanente);
        var documento = mongo.findById(permanente.getId(), Document.class, "sessions");
        assertThat(documento).doesNotContainKey("expireAt");
        // Reproduz o critério de remoção usado pelo índice TTL, sem aguardar seu ciclo.
        mongo.remove(Query.query(Criteria.where("expireAt").lte(new Date())), "sessions");
        assertThat(sessions.findById(permanente.getId()).<String>getAttribute("teste"))
                .isEqualTo("preservado");
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk());

        var temporaria = sessions.createSession();
        temporaria.setMaxInactiveInterval(java.time.Duration.ofSeconds(60));
        temporaria.setLastAccessedTime(Instant.now());
        sessions.save(temporaria);
        assertThat(
                        mongo.findById(temporaria.getId(), Document.class, "sessions")
                                .getDate("expireAt"))
                .isAfter(new Date());
        sessions.deleteById(permanente.getId());
        assertThat(sessions.findById(permanente.getId())).isNull();
    }

    @Test
    void paginasPublicasNaoLiberamApiPrivada() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/empresas/" + own + "/candidatos"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/empresas/" + own + "/agenda"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/api/empresas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/inexistente.js")).andExpect(status().isNotFound());
    }

    @Test
    void transacaoDesfazAlteracaoEEventoSeFalhar() {
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactions)
                                        .executeWithoutResult(
                                                status -> {
                                                    mongo.updateFirst(
                                                            consultaPor("_id", own),
                                                            new Update()
                                                                    .set("nome", "Não persistir"),
                                                            "empresas");
                                                    mongo.insert(
                                                            new Document("_id", UUID.randomUUID())
                                                                    .append("empresa_id", own),
                                                            "eventos_auditoria");
                                                    throw new IllegalStateException(
                                                            "Falha simulada após duas escritas");
                                                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
                        mongo.findOne(consultaPor("_id", own), Document.class, "empresas")
                                .getString("nome"))
                .isEqualTo("Empresa de teste");
        assertThat(mongo.count(new Query(), "eventos_auditoria")).isZero();
    }

    @Test
    void superusuarioGerenciaEmpresasSemVinculoEPerdeAcessoAoSerRevogado() throws Exception {
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        mongo.remove(consultaPor("usuario_id", user), "vinculos_usuarios_empresas");
        mvc.perform(get("/api/empresas").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/empresas/" + other).cookie(session)).andExpect(status().isOk());
        mvc.perform(get("/api/empresas/" + other + "/rascunho").cookie(session))
                .andExpect(status().isOk());
        mvc.perform(
                        put("/api/empresas/" + other)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(business(0, "Cliente administrado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cliente administrado"));
        mvc.perform(
                        put("/api/empresas/" + other + "/rascunho")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(rascunho(0)))
                .andExpect(status().isOk());
        assertThat(
                        mongo.count(
                                consultaPor("empresa_id", other)
                                        .addCriteria(
                                                Criteria.where("usuario_responsavel_id").is(user)),
                                "eventos_auditoria"))
                .isEqualTo(2);
        mvc.perform(get("/api/empresas/" + UUID.randomUUID()).cookie(session))
                .andExpect(status().isNotFound());
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", false), "usuarios");
        mvc.perform(get("/api/empresas").cookie(session))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/empresas/" + other).cookie(session)).andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/empresas/" + other)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(business(1, "Sem acesso")))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginLogoutAndCsrf() throws Exception {
        mvc.perform(get("/api/empresas")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .param("email", "owner@example.test")
                                .param("senha", "errada"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(jsonPath("$.email").value("owner@example.test"));
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .contentType("application/json")
                                .content(business(0, "Meu negócio")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").cookie(session).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
        assertThat(sessions.findByIndexNameAndIndexValue("principalName", "owner@example.test"))
                .isEmpty();
    }

    @Test
    void authenticatedSessionNeverExpiresAndSurvivesRepositoryReload() throws Exception {
        String id =
                new String(
                        Base64.getDecoder().decode(session.getValue()),
                        java.nio.charset.StandardCharsets.UTF_8);
        org.springframework.session.Session stored = sessions.findById(id);
        assertThat(stored).isNotNull();
        assertThat(stored.getMaxInactiveInterval().isNegative()).isTrue();
        assertThat(session.getMaxAge()).isEqualTo(Integer.MAX_VALUE);
        var persisted = sessions.findById(id);
        persisted.setLastAccessedTime(Instant.parse("2000-01-01T00:00:00Z"));
        sessions.save(persisted);
        var repository = new MongoIndexedSessionRepository(mongo);
        repository.setMongoSessionConverter(sessionConverter);
        repository.afterPropertiesSet();
        org.springframework.session.Session reloaded = repository.findById(id);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.isExpired()).isFalse();
        assertThat((Object) reloaded.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").cookie(session).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void forgedCompanyIdsCannotReadOrWrite() throws Exception {
        mvc.perform(get("/api/empresas").cookie(session))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(own.toString()));
        mvc.perform(get("/api/empresas/" + other).cookie(session)).andExpect(status().isNotFound());
        mvc.perform(get("/api/empresas/" + other + "/rascunho").cookie(session))
                .andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/empresas/" + other)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(business(0, "Invadida")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/empresas/" + other + "/rascunho")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(rascunho(0)))
                .andExpect(status().isNotFound());
        assertThat(
                        mongo.findOne(consultaPor("_id", other), Document.class, "empresas")
                                .getString("nome"))
                .isEqualTo("Empresa de teste");
        assertThat(mongo.count(new Query(), "eventos_auditoria")).isZero();
    }

    @Test
    void saveReloadDraftAuditAndStaleRevision() throws Exception {
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(business(0, "Oficina Aurora")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisao").value(1));
        mvc.perform(get("/api/empresas/" + own).cookie(session))
                .andExpect(jsonPath("$.nome").value("Oficina Aurora"))
                .andExpect(jsonPath("$.fusoHorario").value("America/Sao_Paulo"));
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(business(0, "Dados obsoletos")))
                .andExpect(status().isConflict());
        mvc.perform(
                        put("/api/empresas/" + own + "/rascunho")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(rascunho(0)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/empresas/" + own + "/rascunho").cookie(session))
                .andExpect(jsonPath("$.nomeBot").value("Aurora"))
                .andExpect(jsonPath("$.estado").value("rascunho"));
        mvc.perform(
                        put("/api/empresas/" + own + "/rascunho")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(rascunho(0)))
                .andExpect(status().isConflict());
        assertThat(mongo.count(new Query(), "eventos_auditoria")).isEqualTo(2);
        assertThat(mongo.count(consultaPor("estado", "publicado"), "versoes_configuracoes_bot"))
                .isZero();
    }

    @Test
    void invalidInputRollsBack() throws Exception {
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(business(0, " ")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        business(0, "Nome")
                                                .replace("America/Sao_Paulo", "Fuso/Inexistente")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        business(0, "Nome")
                                                .replace(
                                                        "https://example.test",
                                                        "javascript:alert(1)")))
                .andExpect(status().isBadRequest());
        assertThat(
                        mongo.findOne(consultaPor("_id", own), Document.class, "empresas")
                                .getLong("revisao"))
                .isZero();
        assertThat(mongo.count(new Query(), "eventos_auditoria")).isZero();
    }

    @Test
    void painelExigeSuperusuarioERevogacaoValeParaSessaoAberta() throws Exception {
        mvc.perform(get("/api/superusuario/prompt-global")).andExpect(status().isUnauthorized());
        for (String rota :
                List.of(
                        "prompt-global",
                        "prompt-global/historico",
                        "metricas",
                        "prompt-global/previa/" + own)) {
            mvc.perform(get("/api/superusuario/" + rota).cookie(session))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(jsonPath("$.superusuario").value(false));
        mvc.perform(
                        put("/api/superusuario/prompt-global")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(promptGlobal(0, "Regra global")))
                .andExpect(status().isForbidden());
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(jsonPath("$.superusuario").value(true));
        mvc.perform(get("/api/superusuario/metricas").cookie(session)).andExpect(status().isOk());
        mvc.perform(
                        put("/api/superusuario/prompt-global")
                                .cookie(session)
                                .contentType("application/json")
                                .content(promptGlobal(0, "Regra global")))
                .andExpect(status().isForbidden());
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", false), "usuarios");
        for (String rota :
                List.of(
                        "prompt-global",
                        "prompt-global/historico",
                        "metricas",
                        "prompt-global/previa/" + own)) {
            mvc.perform(get("/api/superusuario/" + rota).cookie(session))
                    .andExpect(status().isForbidden());
        }
        assertThat(
                        mongo.findOne(
                                        consultaPor("_id", 1),
                                        Document.class,
                                        "configuracoes_globais")
                                .getLong("revisao"))
                .isZero();
    }

    @Test
    void promptGlobalTemHistoricoPreviaEConflitoDeRevisao() throws Exception {
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        mvc.perform(
                        put("/api/superusuario/prompt-global")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(promptGlobal(0, "Respeite as regras gerais.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisao").value(1))
                .andExpect(jsonPath("$.atualizadoPorEmail").value("owner@example.test"));
        mvc.perform(get("/api/superusuario/prompt-global").cookie(session))
                .andExpect(jsonPath("$.promptSistema").value("Respeite as regras gerais."));
        mvc.perform(get("/api/superusuario/prompt-global/previa/" + own).cookie(session))
                .andExpect(
                        jsonPath("$.promptComposto")
                                .value(
                                        org.hamcrest.Matchers.startsWith(
                                                "Respeite as regras gerais.")))
                .andExpect(
                        jsonPath("$.promptComposto")
                                .value(
                                        org.hamcrest.Matchers.containsString(
                                                "Empresa: Empresa de teste")));
        mvc.perform(
                        put("/api/superusuario/prompt-global")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(promptGlobal(0, "Versão obsoleta")))
                .andExpect(status().isConflict());
        mvc.perform(
                        put("/api/superusuario/prompt-global")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(promptGlobal(1, " ")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/superusuario/prompt-global/historico").cookie(session))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].promptSistema").value("Respeite as regras gerais."));
        assertThat(
                        mongo.findOne(
                                        consultaPor("_id", 1),
                                        Document.class,
                                        "configuracoes_globais")
                                .getLong("revisao"))
                .isEqualTo(1);
    }

    @Test
    void somenteUmUsuarioPodeTerPermissaoSuprema() {
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        UUID segundoUsuario = UUID.randomUUID();
        MongoFixture.usuario(mongo, segundoUsuario, "outro@example.test", "hash", false);
        assertThatThrownBy(
                        () ->
                                mongo.updateFirst(
                                        consultaPor("_id", segundoUsuario),
                                        new Update().set("superusuario", true),
                                        "usuarios"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(mongo.count(consultaPor("superusuario", true), "usuarios")).isEqualTo(1);
    }

    @Test
    void coletaTemCredencialSeparadaEEventosRepetidosNaoContamDuasVezes() throws Exception {
        String evento =
                eventoMensagem(
                        own, "envio-1", "enviada", "sucesso", Instant.now().minusSeconds(20));
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .contentType("application/json")
                                .content(evento))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "errado")
                                .contentType("application/json")
                                .content(evento))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/superusuario/prompt-global")
                                .header("X-Token-Metricas", "credencial-exclusiva-teste-2026"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "credencial-exclusiva-teste-2026")
                                .contentType("application/json")
                                .content(evento))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrado").value(true));
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "credencial-exclusiva-teste-2026")
                                .contentType("application/json")
                                .content(evento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrado").value(false));
        assertThat(mongo.count(new Query(), "eventos_mensagens_atendimento")).isEqualTo(1);
        assertThatThrownBy(
                        () ->
                                new IntegracaoMetricasResource(registroMensagens, "")
                                        .registrar(
                                                "qualquer",
                                                new EventoMensagem(
                                                        own,
                                                        "teste",
                                                        "desabilitado",
                                                        "enviada",
                                                        "sucesso",
                                                        Instant.now())))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("503");
    }

    @Test
    void metricasUsamJanelaDeTempoEErrosNaoSaoEnviosBemSucedidos() throws Exception {
        Instant agora = Instant.now();
        for (String evento :
                List.of(
                        eventoMensagem(
                                own, "recente", "enviada", "sucesso", agora.minusSeconds(20)),
                        eventoMensagem(
                                own, "recebida", "recebida", "sucesso", agora.minusSeconds(3600)),
                        eventoMensagem(
                                other,
                                "historica",
                                "enviada",
                                "sucesso",
                                agora.minusSeconds(172800)),
                        eventoMensagem(own, "falhou", "enviada", "erro", agora.minusSeconds(20)))) {
            mvc.perform(
                            post("/api/integracoes/mensagens")
                                    .header("X-Token-Metricas", "credencial-exclusiva-teste-2026")
                                    .contentType("application/json")
                                    .content(evento))
                    .andExpect(status().isCreated());
        }
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "credencial-exclusiva-teste-2026")
                                .contentType("application/json")
                                .content(
                                        eventoMensagem(
                                                own,
                                                "futuro",
                                                "enviada",
                                                "sucesso",
                                                agora.plusSeconds(86400))))
                .andExpect(status().isBadRequest());
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        mvc.perform(get("/api/superusuario/metricas").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagensUltimoMinuto").value(1))
                .andExpect(jsonPath("$.mensagensUltimas24Horas").value(2))
                .andExpect(jsonPath("$.totalMensagens").value(3))
                .andExpect(jsonPath("$.mensagensRecebidas").value(1))
                .andExpect(jsonPath("$.mensagensEnviadas").value(2))
                .andExpect(jsonPath("$.errosUltimas24Horas").value(1))
                .andExpect(jsonPath("$.volumePorMinuto.length()").value(15))
                .andExpect(jsonPath("$.porEmpresa.length()").value(2));
        assertThat(mongo.count(new Query(), "eventos_mensagens_atendimento")).isEqualTo(4);
    }

    private String promptGlobal(long revisao, String texto) {
        return "{\"promptSistema\":\""
                + texto
                + "\",\"complementarConfiguracoesEmpresa\":true,\"revisao\":"
                + revisao
                + "}";
    }

    private String eventoMensagem(
            UUID empresaId, String eventoId, String direcao, String resultado, Instant ocorridoEm) {
        return "{\"empresaId\":\""
                + empresaId
                + "\",\"origem\":\"teste\",\"idEventoOrigem\":\""
                + eventoId
                + "\",\"direcao\":\""
                + direcao
                + "\",\"resultado\":\""
                + resultado
                + "\",\"ocorridoEm\":\""
                + ocorridoEm
                + "\"}";
    }

    @Test
    void modulosComecamDesabilitadosEClienteNaoPodeHabilitar() throws Exception {
        mvc.perform(get("/api/empresas/" + own).cookie(session))
                .andExpect(jsonPath("$.moduloCandidatosHabilitado").value(false))
                .andExpect(jsonPath("$.moduloAgendaHabilitado").value(false));
        mvc.perform(
                        put("/api/superusuario/empresas/" + own + "/modulos")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(permissoesModulos(0, true, true)))
                .andExpect(status().isForbidden());
        String adulterado =
                business(0, "Nome permitido")
                        .strip()
                        .replaceFirst(
                                "\\{",
                                "{\"moduloCandidatosHabilitado\":true,\"moduloAgendaHabilitado\":true,");
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(adulterado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduloCandidatosHabilitado").value(false))
                .andExpect(jsonPath("$.moduloAgendaHabilitado").value(false));
        mvc.perform(get("/api/empresas/" + own + "/candidatos").cookie(session))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/empresas/" + own + "/agenda").cookie(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void superusuarioConfiguraAsQuatroCombinacoesIndependentes() throws Exception {
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        mongo.remove(consultaPor("usuario_id", user), "vinculos_usuarios_empresas");
        long revisao = 0;
        for (boolean[] combinacao :
                List.of(
                        new boolean[] {true, false},
                        new boolean[] {false, true},
                        new boolean[] {true, true},
                        new boolean[] {false, false})) {
            mvc.perform(
                            put("/api/superusuario/empresas/" + other + "/modulos")
                                    .cookie(session)
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content(
                                            permissoesModulos(
                                                    revisao++, combinacao[0], combinacao[1])))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.moduloCandidatosHabilitado").value(combinacao[0]))
                    .andExpect(jsonPath("$.moduloAgendaHabilitado").value(combinacao[1]));
            mvc.perform(get("/api/empresas/" + other + "/candidatos").cookie(session))
                    .andExpect(status().is(combinacao[0] ? 200 : 403));
            mvc.perform(get("/api/empresas/" + other + "/agenda").cookie(session))
                    .andExpect(status().is(combinacao[1] ? 200 : 403));
        }
        mvc.perform(
                        put("/api/superusuario/empresas/" + other + "/modulos")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(permissoesModulos(0, true, true)))
                .andExpect(status().isConflict());
        assertThat(
                        mongo.count(
                                consultaPor("acao", "permissoes_modulos_atualizadas"),
                                "eventos_auditoria"))
                .isEqualTo(4);
    }

    @Test
    void candidatosPodemSerGerenciadosERegistrosNaoCruzamEmpresas() throws Exception {
        habilitarModulosParaTeste(own, true, false);
        habilitarModulosParaTeste(other, true, true);
        MongoFixture.vinculo(mongo, user, other);
        String resposta =
                mvc.perform(
                                post("/api/empresas/" + own + "/candidatos")
                                        .cookie(session)
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content(candidato(0, false)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        UUID id =
                UUID.fromString(
                        tools.jackson.databind.json.JsonMapper.builder()
                                .build()
                                .readTree(resposta)
                                .get("id")
                                .asText());
        mvc.perform(
                        get("/api/empresas/" + own + "/candidatos")
                                .param("busca", "Ana")
                                .cookie(session))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(
                        get("/api/empresas/" + own + "/candidatos/" + id + "/curriculo")
                                .cookie(session))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.test/curriculo.pdf"));
        mvc.perform(get("/api/empresas/" + other + "/candidatos/" + id).cookie(session))
                .andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/empresas/" + own + "/candidatos/" + id)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(candidato(0, false).replace("novo", "em_analise")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisao").value(1));
        mvc.perform(
                        put("/api/empresas/" + own + "/candidatos/" + id)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(candidato(0, false)))
                .andExpect(status().isConflict());
        mvc.perform(
                        delete("/api/empresas/" + own + "/candidatos/" + id)
                                .param("revisao", "1")
                                .cookie(session)
                                .with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/empresas/" + own + "/candidatos").cookie(session))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(
                        get("/api/empresas/" + own + "/candidatos")
                                .param("incluirArquivados", "true")
                                .cookie(session))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(
                        put("/api/empresas/" + own + "/candidatos/" + id)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(candidato(2, false)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/empresas/" + own + "/candidatos").cookie(session))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void agendaValidaHorariosRevisaoEIsolamento() throws Exception {
        habilitarModulosParaTeste(own, false, true);
        habilitarModulosParaTeste(other, false, true);
        MongoFixture.vinculo(mongo, user, other);
        String resposta =
                mvc.perform(
                                post("/api/empresas/" + own + "/agenda")
                                        .cookie(session)
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content(agendamento(0, false)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        UUID id =
                UUID.fromString(
                        tools.jackson.databind.json.JsonMapper.builder()
                                .build()
                                .readTree(resposta)
                                .get("id")
                                .asText());
        mvc.perform(get("/api/empresas/" + own + "/agenda").cookie(session))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/empresas/" + other + "/agenda/" + id).cookie(session))
                .andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/empresas/" + other + "/agenda/" + id)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(agendamento(0, false)))
                .andExpect(status().isNotFound());
        mvc.perform(
                        delete("/api/empresas/" + other + "/agenda/" + id)
                                .param("revisao", "0")
                                .cookie(session)
                                .with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/empresas/" + own + "/agenda").param("tamanho", "101").cookie(session))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/empresas/" + own + "/agenda")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(agendamento(0, false).replace("11:00:00", "09:00:00")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/api/empresas/" + own + "/agenda/" + id)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(agendamento(0, false).replace("agendado", "confirmado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisao").value(1));
        mvc.perform(
                        put("/api/empresas/" + own + "/agenda/" + id)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(agendamento(0, false)))
                .andExpect(status().isConflict());
        mvc.perform(
                        delete("/api/empresas/" + own + "/agenda/" + id)
                                .param("revisao", "1")
                                .cookie(session)
                                .with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(
                        get("/api/empresas/" + own + "/agenda")
                                .param("incluirArquivados", "true")
                                .cookie(session))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void desligarBloqueiaTodasOperacoesIncluindoN8nEPreservaDados() throws Exception {
        habilitarModulosParaTeste(own, true, true);
        UUID candidatoId = UUID.randomUUID(), agendamentoId = UUID.randomUUID();
        mongo.insert(
                new Document("_id", candidatoId)
                        .append("empresa_id", own)
                        .append("nome_completo", "Preservado")
                        .append("arquivado", false)
                        .append("revisao", 0L),
                "candidatos");
        mongo.insert(
                new Document("_id", agendamentoId)
                        .append("empresa_id", own)
                        .append("nome_pessoa", "Preservado")
                        .append("inicio", new Date())
                        .append("fim", Date.from(Instant.now().plusSeconds(3600)))
                        .append("arquivado", false)
                        .append("revisao", 0L),
                "agendamentos_atendimento");
        habilitarModulosParaTeste(own, false, false);
        for (String prefixo : List.of("/api/empresas/", "/api/integracoes/empresas/")) {
            for (String modulo : List.of("candidatos", "agenda")) {
                String base = prefixo + own + "/" + modulo;
                UUID id = modulo.equals("candidatos") ? candidatoId : agendamentoId;
                String corpo =
                        modulo.equals("candidatos") ? candidato(0, false) : agendamento(0, false);
                mvc.perform(
                                get(base)
                                        .cookie(session)
                                        .header("X-Token-Modulos", "token-modulos-teste-2026"))
                        .andExpect(status().isForbidden());
                mvc.perform(
                                get(base + "/" + id)
                                        .cookie(session)
                                        .header("X-Token-Modulos", "token-modulos-teste-2026"))
                        .andExpect(status().isForbidden());
                mvc.perform(
                                post(base)
                                        .cookie(session)
                                        .with(csrf())
                                        .header("X-Token-Modulos", "token-modulos-teste-2026")
                                        .contentType("application/json")
                                        .content(corpo))
                        .andExpect(status().isForbidden());
                mvc.perform(
                                put(base + "/" + id)
                                        .cookie(session)
                                        .with(csrf())
                                        .header("X-Token-Modulos", "token-modulos-teste-2026")
                                        .contentType("application/json")
                                        .content(corpo))
                        .andExpect(status().isForbidden());
                mvc.perform(
                                delete(base + "/" + id)
                                        .param("revisao", "0")
                                        .cookie(session)
                                        .with(csrf())
                                        .header("X-Token-Modulos", "token-modulos-teste-2026"))
                        .andExpect(status().isForbidden());
            }
        }
        mvc.perform(
                        get("/api/empresas/" + own + "/candidatos/" + candidatoId + "/curriculo")
                                .cookie(session))
                .andExpect(status().isForbidden());
        assertThat(
                        mongo.findOne(consultaPor("_id", candidatoId), Document.class, "candidatos")
                                .getLong("revisao"))
                .isZero();
        assertThat(
                        mongo.findOne(
                                        consultaPor("_id", agendamentoId),
                                        Document.class,
                                        "agendamentos_atendimento")
                                .getLong("revisao"))
                .isZero();
        habilitarModulosParaTeste(own, true, true);
        mvc.perform(get("/api/empresas/" + own + "/candidatos").cookie(session))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/empresas/" + own + "/agenda").cookie(session))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void integracaoNaoUsaCookieNemPodeHabilitarModulos() throws Exception {
        habilitarModulosParaTeste(own, true, true);
        String base = "/api/integracoes/empresas/" + own;
        mvc.perform(get(base + "/candidatos").cookie(session)).andExpect(status().isUnauthorized());
        mvc.perform(
                        post(base + "/candidatos")
                                .header("X-Token-Modulos", "token-modulos-teste-2026")
                                .contentType("application/json")
                                .content(candidato(0, false)))
                .andExpect(status().isCreated());
        mvc.perform(
                        post(base + "/agenda")
                                .header("X-Token-Modulos", "token-modulos-teste-2026")
                                .contentType("application/json")
                                .content(agendamento(0, false)))
                .andExpect(status().isCreated());
        mvc.perform(get(base + "/candidatos").header("X-Token-Modulos", "token-modulos-teste-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(
                        put("/api/superusuario/empresas/" + own + "/modulos")
                                .with(csrf())
                                .header("X-Token-Modulos", "token-modulos-teste-2026")
                                .contentType("application/json")
                                .content(permissoesModulos(0, true, true)))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get(base + "/candidatos")
                                .header("X-Token-Metricas", "credencial-exclusiva-teste-2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuariosEditoresSeguemFlagsDaEmpresaEVinculoAtual() throws Exception {
        habilitarModulosParaTeste(own, true, true);
        UUID editor = UUID.randomUUID();
        MongoFixture.usuario(
                mongo,
                editor,
                "editor@example.test",
                encoder.encode("SenhaEditorTeste-2026"),
                false);
        MongoFixture.vinculo(mongo, editor, own);
        mongo.updateFirst(
                consultaPor("usuario_id", editor).addCriteria(Criteria.where("empresa_id").is(own)),
                new Update().set("papel", "editor"),
                "vinculos_usuarios_empresas");
        Cookie sessaoEditor =
                mvc.perform(
                                post("/api/auth/login")
                                        .with(csrf())
                                        .param("email", "editor@example.test")
                                        .param("senha", "SenhaEditorTeste-2026"))
                        .andExpect(status().isNoContent())
                        .andReturn()
                        .getResponse()
                        .getCookie("SESSION");
        mvc.perform(
                        post("/api/empresas/" + own + "/candidatos")
                                .cookie(sessaoEditor)
                                .with(csrf())
                                .contentType("application/json")
                                .content(candidato(0, false)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/empresas/" + own + "/agenda").cookie(sessaoEditor))
                .andExpect(status().isOk());
        habilitarModulosParaTeste(own, false, true);
        mvc.perform(get("/api/empresas/" + own + "/candidatos").cookie(sessaoEditor))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/empresas/" + own + "/agenda").cookie(sessaoEditor))
                .andExpect(status().isOk());
        mvc.perform(get("/api/empresas/" + own + "/candidatos").cookie(session))
                .andExpect(status().isForbidden());
        mongo.remove(consultaPor("usuario_id", editor), "vinculos_usuarios_empresas");
        mvc.perform(get("/api/empresas/" + own + "/agenda").cookie(sessaoEditor))
                .andExpect(status().isNotFound());
    }

    private void habilitarModulosParaTeste(UUID empresaId, boolean candidatos, boolean agenda) {
        mongo.updateFirst(
                consultaPor("_id", empresaId),
                new Update()
                        .set("modulo_candidatos_habilitado", candidatos)
                        .set("modulo_agenda_habilitado", agenda),
                "empresas");
    }

    private String permissoesModulos(long revisao, boolean candidatos, boolean agenda) {
        return "{\"moduloCandidatosHabilitado\":"
                + candidatos
                + ",\"moduloAgendaHabilitado\":"
                + agenda
                + ",\"revisao\":"
                + revisao
                + "}";
    }

    private String candidato(long revisao, boolean arquivado) {
        return "{\"nomeCompleto\":\"Ana"
                   + " Silva\",\"email\":\"ana@example.test\",\"telefone\":\"123\",\"cargoPretendido\":\"Analista\",\"urlCurriculo\":\"https://example.test/curriculo.pdf\",\"resumoProfissional\":\"Experiência"
                   + " em atendimento\",\"observacoes\":\"\",\"situacao\":\"novo\",\"arquivado\":"
                + arquivado
                + ",\"revisao\":"
                + revisao
                + "}";
    }

    private String agendamento(long revisao, boolean arquivado) {
        return "{\"nomePessoa\":\"Ana"
                   + " Silva\",\"email\":\"ana@example.test\",\"telefone\":\"123\",\"assunto\":\"Consulta\",\"responsavel\":\"Pedro\",\"inicio\":\"2026-10-20T10:00:00Z\",\"fim\":\"2026-10-20T11:00:00Z\",\"situacao\":\"agendado\",\"observacoes\":\"\",\"arquivado\":"
                + arquivado
                + ",\"revisao\":"
                + revisao
                + "}";
    }

    private String business(int revisao, String nome) {
        return """
        {"nome":"%s","descricao":"Negócio local","endereco":"Rua Um","contato":"Contato","site":"https://example.test","servicos":"Reparos","fusoHorario":"America/Sao_Paulo","revisao":%d}
        """
                .formatted(nome, revisao);
    }

    private String rascunho(int revisao) {
        return """
        {"nomeBot":"Aurora","tomVoz":"cordial","usarEmojis":true,"instrucoes":"Cumprimente com clareza","instrucoesTransferencia":"Quando solicitado","revisao":%d}
        """
                .formatted(revisao);
    }

    /** Fails the real audit insert in our random disposable database, after the domain write. */
    @Test
    void falhaNaAuditoriaDesfazEmpresaRascunhoEPermissoes() {
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        var empresaAntes = mongo.findOne(consultaPor("_id", own), Document.class, "empresas");
        var botAntes =
                mongo.findOne(
                        consultaPor("empresa_id", own),
                        Document.class,
                        "versoes_configuracoes_bot");
        mongo.executeCommand(
                new Document("collMod", "eventos_auditoria")
                        .append("validator", new Document("acao", "falha_injetada")));
        try {
            assertThatThrownBy(
                            () ->
                                    servicoEmpresa.salvarEmpresa(
                                            own,
                                            new DadosEmpresa(
                                                    "Não salvar",
                                                    "d",
                                                    "e",
                                                    "c",
                                                    "",
                                                    "s",
                                                    "America/Sao_Paulo",
                                                    0),
                                            "owner@example.test"))
                    .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(
                            () ->
                                    servicoBot.salvarRascunho(
                                            own,
                                            new DadosRascunhoBot(
                                                    "Não salvar",
                                                    "cordial",
                                                    true,
                                                    "instrução",
                                                    "",
                                                    0),
                                            "owner@example.test"))
                    .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(
                            () ->
                                    servicoPermissoes.atualizar(
                                            own,
                                            new PermissoesModulos(true, true, 0),
                                            "owner@example.test"))
                    .isInstanceOf(RuntimeException.class);
            assertThat(mongo.findOne(consultaPor("_id", own), Document.class, "empresas"))
                    .isEqualTo(empresaAntes);
            assertThat(
                            mongo.findOne(
                                    consultaPor("empresa_id", own),
                                    Document.class,
                                    "versoes_configuracoes_bot"))
                    .isEqualTo(botAntes);
            assertThat(mongo.count(new Query(), "eventos_auditoria")).isZero();
        } finally {
            mongo.executeCommand(
                    new Document("collMod", "eventos_auditoria")
                            .append("validator", new Document()));
        }
    }

    @Test
    void falhaNoHistoricoDesfazAtualizacaoDoPrompt() {
        var antes = mongo.findOne(consultaPor("_id", 1), Document.class, "configuracoes_globais");
        mongo.executeCommand(
                new Document("collMod", "historico_prompts_globais")
                        .append("validator", new Document("revisao", -1)));
        try {
            assertThatThrownBy(
                            () ->
                                    servicoPrompt.salvar(
                                            new DadosPromptGlobal("Não persistir", true, 0), user))
                    .isInstanceOf(RuntimeException.class);
            assertThat(
                            mongo.findOne(
                                    consultaPor("_id", 1), Document.class, "configuracoes_globais"))
                    .isEqualTo(antes);
            assertThat(mongo.count(new Query(), "historico_prompts_globais")).isZero();
        } finally {
            mongo.executeCommand(
                    new Document("collMod", "historico_prompts_globais")
                            .append("validator", new Document()));
        }
    }

    @Test
    void respostasExplicitasPreservamJsonCompletoDosDocumentos() throws Exception {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var empresa = repositorioEmpresa.findById(own).orElseThrow();
        empresa.chatwootAccountId = 123L;
        empresa.chatwootInboxId = 456L;
        repositorioEmpresa.save(empresa);
        var bot = repositorioBot.findByEmpresaIdAndEstado(own, "rascunho").orElseThrow();
        var empresaHttp =
                mvc.perform(get("/api/empresas/" + own).cookie(session))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var botHttp =
                mvc.perform(get("/api/empresas/" + own + "/rascunho").cookie(session))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(mapper.readTree(empresaHttp))
                .isEqualTo(mapper.readTree(mapper.writeValueAsString(empresa)));
        assertThat(mapper.readTree(botHttp))
                .isEqualTo(mapper.readTree(mapper.writeValueAsString(bot)));
        assertThat(mapper.readTree(botHttp).has("publicadoEm")).isTrue();
        assertThat(mapper.readTree(botHttp).has("autorId")).isTrue();
    }

    @Test
    void operacaoDeModuloDisputaComDesativacaoEComOutroModulo() throws Exception {
        habilitarModulosParaTeste(own, true, true);
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("superusuario", true), "usuarios");
        var credenciais = Credenciais.usuario(() -> "owner@example.test");
        // Keep a real application operation open, after its access-control write, on a separate
        // transaction.
        for (boolean desativar : List.of(true, false)) {
            var pronta = new java.util.concurrent.CountDownLatch(1);
            var liberar = new java.util.concurrent.CountDownLatch(1);
            try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                var primeira =
                        executor.submit(
                                () ->
                                        new TransactionTemplate(transactions)
                                                .execute(
                                                        status -> {
                                                            servicoCandidatos.listar(
                                                                    own,
                                                                    credenciais,
                                                                    "",
                                                                    false,
                                                                    0,
                                                                    20);
                                                            pronta.countDown();
                                                            try {
                                                                if (!liberar.await(
                                                                        10,
                                                                        java.util.concurrent
                                                                                .TimeUnit
                                                                                .SECONDS)) {
                                                                    throw new IllegalStateException(
                                                                            "Timeout de"
                                                                                + " sincronização");
                                                                }
                                                            } catch (InterruptedException e) {
                                                                Thread.currentThread().interrupt();
                                                                throw new IllegalStateException(e);
                                                            }
                                                            return true;
                                                        }));
                try {
                    assertThat(pronta.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                    Throwable falha =
                            catchThrowable(
                                    () -> {
                                        if (desativar) {
                                            servicoPermissoes.atualizar(
                                                    own,
                                                    new PermissoesModulos(false, true, 0),
                                                    "owner@example.test");
                                        } else {
                                            servicoAgenda.listar(
                                                    own, credenciais, "", false, null, null, 0, 20);
                                        }
                                    });
                    assertThat(falha).isNotNull();
                    Throwable causa = falha;
                    while (causa != null && !(causa instanceof com.mongodb.MongoException))
                        causa = causa.getCause();
                    assertThat(causa).isInstanceOf(com.mongodb.MongoException.class);
                    assertThat(
                                    ((com.mongodb.MongoException) causa)
                                            .hasErrorLabel("TransientTransactionError"))
                            .isTrue();
                } finally {
                    liberar.countDown();
                }
                assertThat(primeira.get(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            }
        }
        assertThat(repositorioEmpresa.findById(own).orElseThrow().moduloCandidatosHabilitado)
                .isTrue();
        // After the competing transaction finishes, deactivation succeeds and the next application
        // call
        // is denied.
        servicoPermissoes.atualizar(
                own, new PermissoesModulos(false, true, 0), "owner@example.test");
        assertThatThrownBy(() -> servicoCandidatos.listar(own, credenciais, "", false, 0, 20))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Módulo desabilitado");
    }

    @Test
    void agregacaoMetricasPreservaJanelasEBucketsComTresComandos() {
        Instant agora = Instant.parse("2026-11-02T12:34:30Z");
        var instantes =
                List.of(
                        agora.minusSeconds(86401),
                        agora.minusSeconds(86400),
                        agora.minusSeconds(61),
                        agora.minusSeconds(60),
                        agora.minusSeconds(30),
                        agora,
                        agora.plusSeconds(1));
        for (UUID empresa : List.of(own, other))
            for (Instant instante : instantes)
                for (String resultado : List.of("sucesso", "erro")) {
                    mongo.insert(
                            new Document("_id", UUID.randomUUID())
                                    .append("empresa_id", empresa)
                                    .append("origem", "limites")
                                    .append("id_evento_origem", UUID.randomUUID().toString())
                                    .append("direcao", "recebida")
                                    .append("resultado", resultado)
                                    .append("ocorrido_em", Date.from(instante)),
                            "eventos_mensagens_atendimento");
                }
        UUID vazia = UUID.randomUUID();
        MongoFixture.empresa(mongo, vazia, user);
        var comandos = new java.util.concurrent.atomic.AtomicInteger();
        var settings =
                com.mongodb.MongoClientSettings.builder()
                        .applyConnectionString(new com.mongodb.ConnectionString(MongoFixture.URI))
                        .uuidRepresentation(org.bson.UuidRepresentation.STANDARD)
                        .addCommandListener(
                                new com.mongodb.event.CommandListener() {
                                    @Override
                                    public void commandStarted(
                                            com.mongodb.event.CommandStartedEvent evento) {
                                        if (List.of("find", "aggregate", "count")
                                                .contains(evento.getCommandName())) {
                                            comandos.incrementAndGet();
                                        }
                                    }
                                })
                        .build();
        try (var cliente = com.mongodb.client.MongoClients.create(settings)) {
            var template = new MongoTemplate(cliente, MongoFixture.DATABASE);
            var referencia = new RepositorioMetricasReferencia(template).consultar(agora);
            assertThat(comandos.get()).isEqualTo(31);
            comandos.set(0);
            var resultado = new MetricasAtendimentoRepository(template).consultar(agora);
            assertThat(comandos.get()).isEqualTo(3);
            assertThat(resultado).isEqualTo(referencia);
            assertThat(resultado.totalMensagens()).isEqualTo(12);
            assertThat(resultado.mensagensUltimas24Horas()).isEqualTo(10);
            assertThat(resultado.mensagensUltimoMinuto()).isEqualTo(6);
            assertThat(resultado.errosUltimas24Horas()).isEqualTo(10);
            assertThat(resultado.mensagensRecebidas()).isEqualTo(12);
            assertThat(resultado.mensagensEnviadas()).isZero();
            assertThat(resultado.ultimaMensagem()).isEqualTo(agora);
            assertThat(resultado.volumePorMinuto()).hasSize(15);
            assertThat(
                            resultado.volumePorMinuto().stream()
                                    .mapToLong(VolumePorMinuto::mensagens)
                                    .sum())
                    .isEqualTo(8);
            assertThat(resultado.porEmpresa()).hasSize(3);
            assertThat(
                            resultado.porEmpresa().stream()
                                    .filter(e -> e.empresaId().equals(vazia))
                                    .findFirst()
                                    .orElseThrow()
                                    .totalMensagens())
                    .isZero();
            assertThat(resultado.porEmpresa().getFirst().totalMensagens()).isEqualTo(6);
            assertThat(resultado.totalUsuariosAtivos()).isEqualTo(1);
        }
    }

    @Test
    void previaPreservaTextoCompletoEDefaultLegadoDeEmojis() {
        mongo.updateFirst(
                consultaPor("_id", 1),
                new Update().set("prompt_sistema", "Regra global"),
                "configuracoes_globais");
        mongo.updateFirst(
                consultaPor("empresa_id", own),
                new Update().unset("usar_emojis"),
                "versoes_configuracoes_bot");
        assertThat(servicoPrompt.visualizarRascunho(own).promptComposto())
                .isEqualTo(
                        "Regra global\n\n"
                            + "CONFIGURAÇÃO COMPLEMENTAR DA EMPRESA\n"
                            + "As regras globais acima têm prioridade sobre estas configurações.\n"
                            + "Empresa: Empresa de teste\n"
                            + "Descrição do negócio: \n"
                            + "Endereço: \n"
                            + "Contatos: \n"
                            + "Site: \n"
                            + "Serviços ou produtos: \n"
                            + "Fuso horário: America/Sao_Paulo\n"
                            + "Nome do assistente: Assistente\n"
                            + "Tom de voz: cordial\n"
                            + "Permitir emojis: não\n"
                            + "Instruções adicionais: \n"
                            + "Transferência para uma pessoa:");
    }

    @Test
    void pacotesNovosLeemEMantemMetadadosPersistidosAnteriores() throws Exception {
        mongo.updateFirst(consultaPor("_id", own), new Update().set("_class", "br.com.painel.empresas.Empresa"), "empresas");
        mongo.updateFirst(consultaPor("empresa_id", own), new Update().set("_class", "br.com.painel.configuracoes.ConfiguracaoBot"), "versoes_configuracoes_bot");
        mvc.perform(get("/api/empresas/" + own).cookie(session)).andExpect(status().isOk());
        mvc.perform(get("/api/empresas/" + own + "/rascunho").cookie(session)).andExpect(status().isOk());
        mvc.perform(put("/api/empresas/" + own).cookie(session).with(csrf()).contentType("application/json").content(business(0, "Metadado preservado"))).andExpect(status().isOk());
        assertThat(mongo.findOne(consultaPor("_id", own), Document.class, "empresas").getString("_class")).isEqualTo("br.com.painel.empresas.Empresa");
        var rascunho = repositorioBot.findByEmpresaIdAndEstado(own, "rascunho").orElseThrow();
        repositorioBot.save(rascunho);
        assertThat(mongo.findOne(consultaPor("empresa_id", own), Document.class, "versoes_configuracoes_bot").getString("_class")).isEqualTo("br.com.painel.configuracoes.ConfiguracaoBot");
    }

}
