package br.com.painel;

import static br.com.painel.persistencia.Documentos.q;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.painel.autenticacao.*;
import br.com.painel.superusuario.*;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.*;
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

@SpringBootTest
@AutoConfigureMockMvc
class BaseIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired MongoTemplate mongo;
  @Autowired PasswordEncoder encoder;
  UUID user, own, other;
  Cookie session;
  @Autowired MongoIndexedSessionRepository sessions;
  @Autowired PlatformTransactionManager transactions;
  @Autowired ServicoRegistroMensagens registroMensagens;

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
                              q("_id", own), new Update().set("nome", "Não persistir"), "empresas");
                          mongo.insert(
                              new Document("_id", UUID.randomUUID()).append("empresa_id", own),
                              "eventos_auditoria");
                          throw new IllegalStateException("Falha simulada após duas escritas");
                        }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(mongo.findOne(q("_id", own), Document.class, "empresas").getString("nome"))
        .isEqualTo("Empresa de teste");
    assertThat(mongo.count(new Query(), "eventos_auditoria")).isZero();
  }

  @Test
  void superusuarioGerenciaEmpresasSemVinculoEPerdeAcessoAoSerRevogado() throws Exception {
    mongo.updateFirst(q("_id", user), new Update().set("superusuario", true), "usuarios");
    mongo.remove(q("usuario_id", user), "vinculos_usuarios_empresas");
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
                q("empresa_id", other)
                    .addCriteria(Criteria.where("usuario_responsavel_id").is(user)),
                "eventos_auditoria"))
        .isEqualTo(2);
    mvc.perform(get("/api/empresas/" + UUID.randomUUID()).cookie(session))
        .andExpect(status().isNotFound());
    mongo.updateFirst(q("_id", user), new Update().set("superusuario", false), "usuarios");
    mvc.perform(get("/api/empresas").cookie(session)).andExpect(jsonPath("$.length()").value(0));
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
    assertThat(mongo.findOne(q("_id", other), Document.class, "empresas").getString("nome"))
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
    assertThat(mongo.count(q("estado", "publicado"), "versoes_configuracoes_bot")).isZero();
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
                .content(business(0, "Nome").replace("America/Sao_Paulo", "Fuso/Inexistente")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/empresas/" + own)
                .cookie(session)
                .with(csrf())
                .contentType("application/json")
                .content(
                    business(0, "Nome").replace("https://example.test", "javascript:alert(1)")))
        .andExpect(status().isBadRequest());
    assertThat(mongo.findOne(q("_id", own), Document.class, "empresas").getLong("revisao"))
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
    mongo.updateFirst(q("_id", user), new Update().set("superusuario", true), "usuarios");
    mvc.perform(get("/api/auth/me").cookie(session))
        .andExpect(jsonPath("$.superusuario").value(true));
    mvc.perform(get("/api/superusuario/metricas").cookie(session)).andExpect(status().isOk());
    mvc.perform(
            put("/api/superusuario/prompt-global")
                .cookie(session)
                .contentType("application/json")
                .content(promptGlobal(0, "Regra global")))
        .andExpect(status().isForbidden());
    mongo.updateFirst(q("_id", user), new Update().set("superusuario", false), "usuarios");
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
            mongo.findOne(q("_id", 1), Document.class, "configuracoes_globais").getLong("revisao"))
        .isZero();
  }

  @Test
  void promptGlobalTemHistoricoPreviaEConflitoDeRevisao() throws Exception {
    mongo.updateFirst(q("_id", user), new Update().set("superusuario", true), "usuarios");
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
                .value(org.hamcrest.Matchers.startsWith("Respeite as regras gerais.")))
        .andExpect(
            jsonPath("$.promptComposto")
                .value(org.hamcrest.Matchers.containsString("Empresa: Empresa de teste")));
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
            mongo.findOne(q("_id", 1), Document.class, "configuracoes_globais").getLong("revisao"))
        .isEqualTo(1);
  }

  @Test
  void somenteUmUsuarioPodeTerPermissaoSuprema() {
    mongo.updateFirst(q("_id", user), new Update().set("superusuario", true), "usuarios");
    UUID segundoUsuario = UUID.randomUUID();
    MongoFixture.usuario(mongo, segundoUsuario, "outro@example.test", "hash", false);
    assertThatThrownBy(
            () ->
                mongo.updateFirst(
                    q("_id", segundoUsuario), new Update().set("superusuario", true), "usuarios"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(mongo.count(q("superusuario", true), "usuarios")).isEqualTo(1);
  }

  @Test
  void coletaTemCredencialSeparadaEEventosRepetidosNaoContamDuasVezes() throws Exception {
    String evento =
        eventoMensagem(own, "envio-1", "enviada", "sucesso", Instant.now().minusSeconds(20));
    mvc.perform(post("/api/integracoes/mensagens").contentType("application/json").content(evento))
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
                new ControladorIntegracaoMetricas(registroMensagens, "")
                    .registrar(
                        "qualquer",
                        new ServicoRegistroMensagens.EventoMensagem(
                            own, "teste", "desabilitado", "enviada", "sucesso", Instant.now())))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("503");
  }

  @Test
  void metricasUsamJanelaDeTempoEErrosNaoSaoEnviosBemSucedidos() throws Exception {
    Instant agora = Instant.now();
    for (String evento :
        List.of(
            eventoMensagem(own, "recente", "enviada", "sucesso", agora.minusSeconds(20)),
            eventoMensagem(own, "recebida", "recebida", "sucesso", agora.minusSeconds(3600)),
            eventoMensagem(other, "historica", "enviada", "sucesso", agora.minusSeconds(172800)),
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
                    eventoMensagem(own, "futuro", "enviada", "sucesso", agora.plusSeconds(86400))))
        .andExpect(status().isBadRequest());
    mongo.updateFirst(q("_id", user), new Update().set("superusuario", true), "usuarios");
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
                "\\{", "{\"moduloCandidatosHabilitado\":true,\"moduloAgendaHabilitado\":true,");
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
    mongo.updateFirst(q("_id", user), new Update().set("superusuario", true), "usuarios");
    mongo.remove(q("usuario_id", user), "vinculos_usuarios_empresas");
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
                  .content(permissoesModulos(revisao++, combinacao[0], combinacao[1])))
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
    assertThat(mongo.count(q("acao", "permissoes_modulos_atualizadas"), "eventos_auditoria"))
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
    mvc.perform(get("/api/empresas/" + own + "/candidatos").param("busca", "Ana").cookie(session))
        .andExpect(jsonPath("$.total").value(1));
    mvc.perform(get("/api/empresas/" + own + "/candidatos/" + id + "/curriculo").cookie(session))
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
        String corpo = modulo.equals("candidatos") ? candidato(0, false) : agendamento(0, false);
        mvc.perform(get(base).cookie(session).header("X-Token-Modulos", "token-modulos-teste-2026"))
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
            mongo.findOne(q("_id", candidatoId), Document.class, "candidatos").getLong("revisao"))
        .isZero();
    assertThat(
            mongo
                .findOne(q("_id", agendamentoId), Document.class, "agendamentos_atendimento")
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
            get(base + "/candidatos").header("X-Token-Metricas", "credencial-exclusiva-teste-2026"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void usuariosEditoresSeguemFlagsDaEmpresaEVinculoAtual() throws Exception {
    habilitarModulosParaTeste(own, true, true);
    UUID editor = UUID.randomUUID();
    MongoFixture.usuario(
        mongo, editor, "editor@example.test", encoder.encode("SenhaEditorTeste-2026"), false);
    MongoFixture.vinculo(mongo, editor, own);
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
    mongo.remove(q("usuario_id", editor), "vinculos_usuarios_empresas");
    mvc.perform(get("/api/empresas/" + own + "/agenda").cookie(sessaoEditor))
        .andExpect(status().isNotFound());
  }

  private void habilitarModulosParaTeste(UUID empresaId, boolean candidatos, boolean agenda) {
    mongo.updateFirst(
        q("_id", empresaId),
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
}
