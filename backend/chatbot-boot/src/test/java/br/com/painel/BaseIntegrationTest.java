package br.com.painel;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.painel.autenticacao.model.*;
import br.com.painel.core.service.MigracaoModelagemService;
import br.com.painel.empresas.model.Empresa;
import br.com.painel.superusuario.repository.MetricasAtendimentoRepository;

import jakarta.servlet.http.Cookie;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
class BaseIntegrationTest {

    @Autowired MockMvc mvc;

    @Autowired MongoTemplate mongo;

    @Autowired PasswordEncoder encoder;

    @Autowired MigracaoModelagemService migracao;

    @Autowired MetricasAtendimentoRepository metricas;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("prepararMongo")
    ApplicationRunner preparar;

    UUID user, admin, own, other;
    Cookie session, master;

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.mongodb.uri", () -> MongoFixture.URI);
        r.add("server.servlet.session.cookie.secure", () -> false);
        r.add("painel.integracao.token-metricas", () -> "token-teste");
    }

    @AfterAll
    static void limpar(@Autowired MongoTemplate mongo) {
        mongo.getDb().drop();
    }

    @BeforeEach
    void setup() throws Exception {
        for (var c : mongo.getCollectionNames()) {
            mongo.remove(new Query(), c);
        }
        preparar.run(null);
        user = UUID.randomUUID();
        admin = UUID.randomUUID();
        own = UUID.randomUUID();
        other = UUID.randomUUID();
        MongoFixture.usuario(
                mongo, user, "owner@example.test", encoder.encode("SenhaTeste2026"), false);
        MongoFixture.usuario(
                mongo, admin, "admin@example.test", encoder.encode("SenhaTeste2026"), true);
        MongoFixture.empresa(mongo, own, user);
        MongoFixture.empresa(mongo, other, admin);
        MongoFixture.vinculo(mongo, user, own);
        session = login("owner@example.test");
        master = login("admin@example.test");
    }

    private Cookie login(String email) throws Exception {
        return mvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .param("email", email)
                                .param("senha", "SenhaTeste2026"))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("SESSION");
    }

    private String json(Object valor) {
        return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(valor);
    }

    private Document dadosEmpresa(long revisao) {
        return new Document("nome", "Empresa editada")
                .append("descricao", "Descrição")
                .append("endereco", "Rua 1")
                .append("contato", "Contato")
                .append("site", "https://example.test")
                .append("servicos", "Serviços")
                .append("fusoHorario", "America/Sao_Paulo")
                .append("revisao", revisao);
    }

    private Document dadosBot(long revisao) {
        return new Document("nome", "Aurora")
                .append("tomVoz", "cordial")
                .append("usarEmojis", true)
                .append("instrucoes", "Cumprimente")
                .append("instrucoesTransferencia", "Quando solicitado")
                .append("revisao", revisao);
    }

    @Test
    void identidadeUsaPerfilEUsuarioAcessaSomenteSuaEmpresa() throws Exception {
        mvc.perform(get("/api/auth/me").cookie(master))
                .andExpect(jsonPath("$.perfil").value("SUPERUSUARIO"));
        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(jsonPath("$.perfil").value("USUARIO"));
        mvc.perform(get("/api/empresas").cookie(session))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/empresas").cookie(master)).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/empresas/" + other).cookie(session)).andExpect(status().isNotFound());
        mvc.perform(
                        put("/api/empresas/" + other)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dadosEmpresa(0))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/superusuario/metricas").cookie(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void revogacaoDePerfilOuEmpresaValeNaSessaoAberta() throws Exception {
        mongo.updateFirst(
                consultaPor("_id", admin),
                new Update().set("perfil", "USUARIO").set("empresaId", other),
                Usuario.class);
        mvc.perform(get("/api/superusuario/prompt-global").cookie(master))
                .andExpect(status().isForbidden());
        mongo.updateFirst(
                consultaPor("_id", user), new Update().set("empresaId", other), Usuario.class);
        mvc.perform(get("/api/empresas/" + own).cookie(session)).andExpect(status().isNotFound());
        mvc.perform(get("/api/empresas/" + other).cookie(session)).andExpect(status().isOk());
    }

    @Test
    void csrfLogoutESessaoPermanente() throws Exception {
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .contentType("application/json")
                                .content(json(dadosEmpresa(0))))
                .andExpect(status().isForbidden());
        var sessions = mongo.find(new Query(), Document.class, "sessions");
        assertThat(sessions).isNotEmpty();
        assertThat(sessions).allSatisfy(s -> assertThat(s.get("expireAt")).isNull());
        mvc.perform(post("/api/auth/logout").cookie(session).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void empresaEBotUsamMesmoDocumentoERevisao() throws Exception {
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dadosEmpresa(0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisao").value(1))
                .andExpect(jsonPath("$.bot.nome").value("Assistente"));
        mvc.perform(
                        put("/api/empresas/" + own + "/bot")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dadosBot(0))))
                .andExpect(status().isConflict());
        mvc.perform(
                        put("/api/empresas/" + own + "/bot")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dadosBot(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bot.nome").value("Aurora"))
                .andExpect(jsonPath("$.nome").value("Empresa editada"))
                .andExpect(jsonPath("$.revisao").value(2));
        var e = mongo.findById(own, Document.class, "empresas");
        assertThat(e.get("bot", Document.class).getString("nome")).isEqualTo("Aurora");
        assertThat(mongo.collectionExists("versoes_configuracoes_bot")).isFalse();
    }

    @Test
    void questionarioGeraInstrucoesEPreservaTextoLivreComIsolamento() throws Exception {
        var dados = dadosBot(0).append("modoGuiado", true).append("tomVoz", "direto")
            .append("questionario", new Document("pagamentos", "Pix e cartão")
                .append("entrega", "Retirada na loja").append("chamarPessoa", "Quando solicitado"));
        mvc.perform(put("/api/empresas/" + other + "/bot").cookie(session).with(csrf())
                .contentType("application/json").content(json(dados)))
            .andExpect(status().isNotFound());
        mvc.perform(put("/api/empresas/" + own + "/bot").cookie(session).with(csrf())
                .contentType("application/json").content(json(dados)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bot.instrucoesLivres").value("Cumprimente"))
            .andExpect(jsonPath("$.bot.instrucoes", org.hamcrest.Matchers.containsString("Pagamento: Pix e cartão")))
            .andExpect(jsonPath("$.bot.instrucoesTransferencia").value("Quando solicitado"));
        mvc.perform(get("/api/empresas/" + own).cookie(session))
            .andExpect(jsonPath("$.bot.questionario.pagamentos").value("Pix e cartão"));
        dados.append("modoGuiado", false).append("revisao", 1);
        mvc.perform(put("/api/empresas/" + own + "/bot").cookie(session).with(csrf())
                .contentType("application/json").content(json(dados)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bot.instrucoes").value("Cumprimente"))
            .andExpect(jsonPath("$.bot.questionario.pagamentos").value("Pix e cartão"));
    }

    @Test
    void rejeitaQuestionarioExcessivoENaoInventaRespostasVazias() throws Exception {
        var dados = dadosBot(0).append("modoGuiado", true)
            .append("questionario", new Document("pagamentos", "x".repeat(1501)));
        mvc.perform(put("/api/empresas/" + own + "/bot").cookie(session).with(csrf())
                .contentType("application/json").content(json(dados)))
            .andExpect(status().isBadRequest());
        dados.append("questionario", new Document());
        mvc.perform(put("/api/empresas/" + own + "/bot").cookie(session).with(csrf())
                .contentType("application/json").content(json(dados)))
            .andExpect(status().isOk());
        var texto = mongo.findById(own, Empresa.class).bot.instrucoes;
        assertThat(texto).contains("Não invente").doesNotContain("Pagamento:", "Entrega e retirada:");
    }

    @Test
    void dadosInvalidosNaoAlteramEmpresa() throws Exception {
        var dados = dadosEmpresa(0).append("fusoHorario", "Inválido");
        mvc.perform(
                        put("/api/empresas/" + own)
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/api/empresas/" + own + "/bot")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dadosBot(0).append("nome", ""))))
                .andExpect(status().isBadRequest());
        assertThat(mongo.findById(own, Empresa.class).revisao).isZero();
    }

    @Test
    void chatwootFicaNaEmpresaComExclusividadeERevisao() throws Exception {
        var dados =
                new Document("chatwootAccountId", 1)
                        .append("chatwootInboxId", 2)
                        .append("revisao", 0);
        mvc.perform(
                        put("/api/superusuario/empresas/" + own + "/chatwoot")
                                .cookie(session)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/superusuario/empresas/" + own + "/chatwoot")
                                .cookie(master)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatwootAccountId").value(1));
        mvc.perform(
                        put("/api/superusuario/empresas/" + other + "/chatwoot")
                                .cookie(master)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isConflict());
        mvc.perform(
                        put("/api/superusuario/empresas/" + own + "/chatwoot")
                                .cookie(master)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isConflict());
    }

    private Document criarConvite(String email) throws Exception {
        var body =
                new Document("nome", "Empresa convidada")
                        .append("responsavel", "Cliente")
                        .append("email", email);
        String resposta =
                mvc.perform(
                                post("/api/superusuario/empresas")
                                        .cookie(master)
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content(json(body)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return Document.parse(resposta);
    }

    private Document ativacao(String token) {
        return new Document("token", token)
                .append("email", "cliente@example.test")
                .append("senha", "SenhaCliente2026")
                .append("confirmacaoSenha", "SenhaCliente2026");
    }

    @Test
    void conviteEmbutidoAtivaUmaVezSemExporSegredos() throws Exception {
        var c = criarConvite("cliente@example.test");
        var u = mongo.findOne(consultaPor("email", "cliente@example.test"), Usuario.class);
        assertThat(u.perfil).isEqualTo(PerfilUsuario.USUARIO);
        assertThat(u.convite.tokenHash).isNotEqualTo(c.getString("token"));
        assertThat(u.empresaId.toString()).isEqualTo(c.getString("empresaId"));
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(ativacao(c.getString("token")))))
                .andExpect(status().isNoContent());
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(ativacao(c.getString("token")))))
                .andExpect(status().isBadRequest());
        var ativado = mongo.findById(u.id, Usuario.class);
        assertThat(ativado.ativo).isTrue();
        assertThat(encoder.matches("SenhaCliente2026", ativado.hashSenha)).isTrue();
        mvc.perform(get("/api/empresas/" + u.empresaId).cookie(master))
                .andExpect(jsonPath("$.convite").doesNotExist())
                .andExpect(jsonPath("$.hashSenha").doesNotExist());
        assertThat(mongo.collectionExists("convites_empresas")).isFalse();
    }

    @Test
    void reenvioRevogaTokenAnteriorEExpiradoNaoAtiva() throws Exception {
        var c = criarConvite("cliente@example.test");
        var empresa = c.getString("empresaId");
        String novo =
                mvc.perform(
                                post("/api/superusuario/empresas/" + empresa + "/convite")
                                        .cookie(master)
                                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(ativacao(c.getString("token")))))
                .andExpect(status().isBadRequest());
        mongo.updateFirst(
                consultaPor("email", "cliente@example.test"),
                new Update().set("convite.expiraEm", Instant.now().minusSeconds(1)),
                Usuario.class);
        mvc.perform(
                        post("/api/auth/ativar-convite")
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(ativacao(Document.parse(novo).getString("token")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailExistenteNaoCriaEmpresaParcial() throws Exception {
        var antes = mongo.count(new Query(), Empresa.class);
        mvc.perform(
                        post("/api/superusuario/empresas")
                                .cookie(master)
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        json(
                                                new Document("nome", "Repetida")
                                                        .append("responsavel", "Cliente")
                                                        .append("email", "owner@example.test"))))
                .andExpect(status().isConflict());
        assertThat(mongo.count(new Query(), Empresa.class)).isEqualTo(antes);
    }

    @Test
    void promptGlobalSemHistoricoMantemRevisaoEPrevia() throws Exception {
        var dados =
                new Document("promptGlobal", "Regra global")
                        .append("complementarConfiguracoesEmpresa", true)
                        .append("revisao", 0);
        mvc.perform(
                        put("/api/superusuario/prompt-global")
                                .cookie(master)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptGlobal").value("Regra global"))
                .andExpect(jsonPath("$.revisao").value(1));
        mvc.perform(
                        put("/api/superusuario/prompt-global")
                                .cookie(master)
                                .with(csrf())
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/superusuario/prompt-global/previa/" + own).cookie(master))
                .andExpect(
                        jsonPath("$.promptComposto")
                                .value(
                                        org.hamcrest.Matchers.containsString(
                                                "Nome do assistente: Assistente")));
        mvc.perform(get("/api/superusuario/prompt-global/historico").cookie(master))
                .andExpect(status().isNotFound());
        assertThat(mongo.collectionExists("historico_prompts_globais")).isFalse();
    }

    private Document mensagem(UUID empresa, Instant data) {
        return new Document("empresaId", empresa.toString()).append("enviadoEm", data.toString());
    }

    @Test
    void mensagensTemSomenteTresCamposEDuplicataNaoContaDuasVezes() throws Exception {
        var id = new ObjectId().toHexString();
        var dados = mensagem(own, Instant.now().minusSeconds(10));
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "token-teste")
                                .header("Idempotency-Key", id)
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isCreated());
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "token-teste")
                                .header("Idempotency-Key", id)
                                .contentType("application/json")
                                .content(json(dados)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrado").value(false));
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "token-teste")
                                .header("Idempotency-Key", id)
                                .contentType("application/json")
                                .content(json(mensagem(other, Instant.now().minusSeconds(10)))))
                .andExpect(status().isConflict());
        var salvo = mongo.findById(new ObjectId(id), Document.class, "eventos_mensagens");
        assertThat(salvo.keySet()).containsExactlyInAnyOrder("_id", "empresaId", "enviadoEm");
        assertThat(salvo.get("enviadoEm")).isInstanceOf(Date.class);
        assertThat(mongo.count(new Query(), "eventos_mensagens")).isEqualTo(1);
    }

    @Test
    void coletaExigeTokenIdValidoEmpresaEDataPassada() throws Exception {
        var id = new ObjectId().toHexString();
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .cookie(master)
                                .header("Idempotency-Key", id)
                                .contentType("application/json")
                                .content(json(mensagem(own, Instant.now()))))
                .andExpect(status().isUnauthorized());
        for (var dados :
                List.of(
                        mensagem(own, Instant.now().plusSeconds(60)),
                        new Document("empresaId", own.toString()))) {
            mvc.perform(
                            post("/api/integracoes/mensagens")
                                    .header("X-Token-Metricas", "token-teste")
                                    .header("Idempotency-Key", id)
                                    .contentType("application/json")
                                    .content(json(dados)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "token-teste")
                                .header("Idempotency-Key", "invalido")
                                .contentType("application/json")
                                .content(json(mensagem(own, Instant.now()))))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/integracoes/mensagens")
                                .header("X-Token-Metricas", "token-teste")
                                .header("Idempotency-Key", id)
                                .contentType("application/json")
                                .content(json(mensagem(UUID.randomUUID(), Instant.now()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void metricasFiltramEmpresaPeriodoEContamPorMinutoHoraDia() {
        var agora = Instant.parse("2026-11-04T15:30:00Z");
        for (var empresa : List.of(own, other)) {
            for (var segundos : List.of(10L, 20L, 70L, 3700L)) {
                mongo.insert(
                        new Document("_id", new ObjectId())
                                .append("empresaId", empresa)
                                .append("enviadoEm", Date.from(agora.minusSeconds(segundos))),
                        "eventos_mensagens");
            }
        }
        var minuto =
                metricas.consultar(
                        agora, agora.minusSeconds(120), agora, "minute", own, "America/Sao_Paulo");
        assertThat(minuto.totalMensagens()).isEqualTo(4);
        assertThat(minuto.mensagensUltimoMinuto()).isEqualTo(2);
        assertThat(minuto.volume()).extracting(v -> v.mensagens()).containsExactly(1L, 2L);
        var hora = metricas.consultar(agora, agora.minusSeconds(7200), agora, "hour", null, "UTC");
        assertThat(hora.volume().stream().mapToLong(v -> v.mensagens()).sum()).isEqualTo(8);
        var dia =
                metricas.consultar(
                        agora, agora.minusSeconds(86400), agora, "day", own, "America/Sao_Paulo");
        assertThat(dia.volume().stream().mapToLong(v -> v.mensagens()).sum()).isEqualTo(4);
    }

    @Test
    void filtrosInvalidosSaoRecusadosENaoHaCamposDeErroOuRecebimento() throws Exception {
        mvc.perform(get("/api/superusuario/metricas").cookie(master).param("intervalo", "invalido"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        get("/api/superusuario/metricas")
                                .cookie(master)
                                .param("de", "2020-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/superusuario/metricas").cookie(master))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMensagens").value(0))
                .andExpect(
                        jsonPath("$.volume.length()")
                                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(15)))
                .andExpect(jsonPath("$.mensagensRecebidas").doesNotExist())
                .andExpect(jsonPath("$.errosUltimas24Horas").doesNotExist());
    }

    @Test
    void inicializacaoMantemSomenteQuatroColecoesDeDominio() throws Exception {
        preparar.run(null);
        assertThat(mongo.getCollectionNames())
                .contains("empresas", "usuarios", "configuracoes_sistema", "eventos_mensagens")
                .doesNotContain(
                        "eventos_auditoria",
                        "convites_empresas",
                        "vinculos_usuarios_empresas",
                        "versoes_configuracoes_bot",
                        "historico_prompts_globais");
    }

    private void legado(boolean multiplasEmpresas) {
        mongo.remove(new Query(), "usuarios");
        mongo.remove(new Query(), "empresas");
        mongo.remove(new Query(), "configuracoes_sistema");
        for (var nome :
                List.of(
                        "vinculos_usuarios_empresas",
                        "versoes_configuracoes_bot",
                        "convites_empresas",
                        "configuracoes_globais",
                        "eventos_mensagens_atendimento",
                        "eventos_auditoria")) {
            if (!mongo.collectionExists(nome)) {
                mongo.createCollection(nome);
            }
        }
        mongo.insert(
                new Document("_id", user)
                        .append("email", "legado@example.test")
                        .append("superusuario", false)
                        .append("hash_senha", "hash-preservado")
                        .append("ativo", false),
                "usuarios");
        mongo.insert(
                new Document("_id", own)
                        .append("nome", "Legada")
                        .append("fuso_horario", "America/Sao_Paulo")
                        .append("chatwoot_account_id", 7L)
                        .append("chatwoot_inbox_id", 8L)
                        .append("revisao", 3L),
                "empresas");
        mongo.insert(new Document("_id", other).append("nome", "Outra"), "empresas");
        mongo.insert(
                new Document("_id", new ObjectId())
                        .append("usuario_id", user)
                        .append("empresa_id", own),
                "vinculos_usuarios_empresas");
        if (multiplasEmpresas) {
            mongo.insert(
                    new Document("_id", new ObjectId())
                            .append("usuario_id", user)
                            .append("empresa_id", other),
                    "vinculos_usuarios_empresas");
        }
        mongo.insert(
                new Document("_id", new ObjectId())
                        .append("empresa_id", own)
                        .append("estado", "rascunho")
                        .append("nome_bot", "Bot legado")
                        .append("tom_voz", "formal")
                        .append("usar_emojis", true),
                "versoes_configuracoes_bot");
        mongo.insert(
                new Document("_id", new ObjectId())
                        .append("usuario_id", user)
                        .append("empresa_id", own)
                        .append("token_hash", "token-preservado")
                        .append("expira_em", Date.from(Instant.now().plusSeconds(3600))),
                "convites_empresas");
        mongo.insert(
                new Document("_id", 1)
                        .append("prompt_sistema", "Prompt legado")
                        .append("revisao", 4L),
                "configuracoes_globais");
        for (var direcao : List.of("enviada", "recebida")) {
            mongo.insert(
                    new Document("_id", UUID.randomUUID())
                            .append("empresa_id", own)
                            .append("direcao", direcao)
                            .append("resultado", "sucesso")
                            .append("ocorrido_em", new Date()),
                    "eventos_mensagens_atendimento");
        }
    }

    @Test
    void migracaoPreservaCredenciaisBotConviteChatwootPromptEContagem() throws Exception {
        legado(false);
        preparar.run(null);
        var u = mongo.findById(user, Usuario.class);
        var e = mongo.findById(own, Empresa.class);
        assertThat(u.empresaId).isEqualTo(own);
        assertThat(u.hashSenha).isEqualTo("hash-preservado");
        assertThat(u.convite.tokenHash).isEqualTo("token-preservado");
        assertThat(u.perfil).isEqualTo(PerfilUsuario.USUARIO);
        assertThat(e.bot.nome).isEqualTo("Bot legado");
        assertThat(e.chatwootAccountId).isEqualTo(7L);
        assertThat(e.revisao).isEqualTo(3);
        assertThat(
                        mongo.findById("principal", Document.class, "configuracoes_sistema")
                                .getString("promptGlobal"))
                .isEqualTo("Prompt legado");
        assertThat(mongo.count(new Query(), "eventos_mensagens")).isEqualTo(1);
        preparar.run(null);
        assertThat(mongo.count(new Query(), "eventos_mensagens")).isEqualTo(1);
        assertThat(mongo.collectionExists("eventos_auditoria")).isFalse();
    }

    @Test
    void migracaoComMultiplasEmpresasAbortaSemAlterarDados() {
        legado(true);
        assertThatThrownBy(() -> migracao.converter()).hasMessageContaining("múltiplas empresas");
        assertThat(mongo.findById(user, Document.class, "usuarios").getString("hash_senha"))
                .isEqualTo("hash-preservado");
        assertThat(mongo.collectionExists("convites_empresas")).isTrue();
        assertThat(mongo.count(new Query(), "eventos_mensagens")).isZero();
    }
}
