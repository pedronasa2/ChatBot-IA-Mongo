package br.com.painel;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.painel.autenticacao.*;
import br.com.painel.superusuario.*;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class BaseIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  UUID user, own, other;
  Cookie session;
  @Autowired JdbcIndexedSessionRepository sessions;
  @Autowired PlatformTransactionManager transactions;
  @Autowired ServicoRegistroMensagens registroMensagens;

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> PostgresFixture.DB.getJdbcUrl("postgres", "postgres"));
    r.add("spring.datasource.username", () -> "postgres");
    r.add("spring.datasource.password", () -> "");
    r.add("server.servlet.session.cookie.secure", () -> false);
    r.add("painel.integracao.token-metricas", () -> "credencial-exclusiva-teste-2026");
  }

  @BeforeEach
  void setup() throws Exception {
    jdbc.execute(
        "truncate spring_session,"
            + " eventos_auditoria,versoes_configuracoes_bot,vinculos_usuarios_empresas,empresas,usuarios"
            + " cascade");
    jdbc.update("insert into configuracoes_globais(id) values(1)");
    user = UUID.randomUUID();
    own = UUID.randomUUID();
    other = UUID.randomUUID();
    jdbc.update(
        "insert into usuarios(id,email,hash_senha) values(?,?,?)",
        user,
        "owner@example.test",
        encoder.encode("SenhaSomenteTeste-2026"));
    for (UUID id : List.of(own, other)) {
      jdbc.update("insert into empresas(id,nome) values(?,'Empresa de teste')", id);
      jdbc.update(
          "insert into versoes_configuracoes_bot(id,empresa_id,versao,estado,autor_id)"
              + " values(?,?,1,'rascunho',?)",
          UUID.randomUUID(),
          id,
          user);
    }
    jdbc.update(
        "insert into vinculos_usuarios_empresas(id,usuario_id,empresa_id,papel) values(?,?,?,'proprietario')",
        UUID.randomUUID(),
        user,
        own);
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
  void superusuarioGerenciaEmpresasSemVinculoEPerdeAcessoAoSerRevogado() throws Exception {
    jdbc.update("update usuarios set superusuario=true where id=?", user);
    jdbc.update("delete from vinculos_usuarios_empresas where usuario_id=?", user);
    mvc.perform(get("/api/empresas").cookie(session))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    mvc.perform(get("/api/empresas/" + other).cookie(session)).andExpect(status().isOk());
    mvc.perform(get("/api/empresas/" + other + "/rascunho").cookie(session)).andExpect(status().isOk());
    mvc.perform(put("/api/empresas/" + other).cookie(session).with(csrf())
        .contentType("application/json").content(business(0, "Cliente administrado")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Cliente administrado"));
    mvc.perform(put("/api/empresas/" + other + "/rascunho").cookie(session).with(csrf())
        .contentType("application/json").content(rascunho(0))).andExpect(status().isOk());
    assertThat(jdbc.queryForObject("select count(*) from eventos_auditoria where empresa_id=? and usuario_responsavel_id=?",
        Long.class, other, user)).isEqualTo(2);
    mvc.perform(get("/api/empresas/" + UUID.randomUUID()).cookie(session)).andExpect(status().isNotFound());
    jdbc.update("update usuarios set superusuario=false where id=?", user);
    mvc.perform(get("/api/empresas").cookie(session)).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/empresas/" + other).cookie(session)).andExpect(status().isNotFound());
    mvc.perform(put("/api/empresas/" + other).cookie(session).with(csrf())
        .contentType("application/json").content(business(1, "Sem acesso"))).andExpect(status().isNotFound());
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
    assertThat(
            jdbc.queryForObject(
                "select count(*) from spring_session where principal_name=?",
                Long.class,
                "owner@example.test"))
        .isZero();
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
    jdbc.update(
        "update spring_session set last_access_time=? where session_id=?",
        Instant.parse("2000-01-01T00:00:00Z").toEpochMilli(),
        id);
    sessions.cleanUpExpiredSessions();
    org.springframework.session.Session reloaded =
        new JdbcIndexedSessionRepository(jdbc, new TransactionTemplate(transactions)).findById(id);
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
    assertThat(jdbc.queryForObject("select nome from empresas where id=?", String.class, other))
        .isEqualTo("Empresa de teste");
    assertThat(jdbc.queryForObject("select count(*) from eventos_auditoria", Long.class)).isZero();
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
    assertThat(jdbc.queryForObject("select count(*) from eventos_auditoria", Long.class)).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from versoes_configuracoes_bot where estado='publicado'",
                Long.class))
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
    assertThat(jdbc.queryForObject("select revisao from empresas where id=?", Long.class, own))
        .isZero();
    assertThat(jdbc.queryForObject("select count(*) from eventos_auditoria", Long.class)).isZero();
  }

  @Test
  void painelExigeSuperusuarioERevogacaoValeParaSessaoAberta() throws Exception {
    mvc.perform(get("/api/superusuario/prompt-global")).andExpect(status().isUnauthorized());
    for (String rota : List.of("prompt-global", "prompt-global/historico", "metricas", "prompt-global/previa/" + own)) {
      mvc.perform(get("/api/superusuario/" + rota).cookie(session)).andExpect(status().isForbidden());
    }
    mvc.perform(get("/api/auth/me").cookie(session)).andExpect(jsonPath("$.superusuario").value(false));
    mvc.perform(put("/api/superusuario/prompt-global").cookie(session).with(csrf())
        .contentType("application/json").content(promptGlobal(0, "Regra global")))
        .andExpect(status().isForbidden());
    jdbc.update("update usuarios set superusuario=true where id=?", user);
    mvc.perform(get("/api/auth/me").cookie(session)).andExpect(jsonPath("$.superusuario").value(true));
    mvc.perform(get("/api/superusuario/metricas").cookie(session)).andExpect(status().isOk());
    mvc.perform(put("/api/superusuario/prompt-global").cookie(session)
        .contentType("application/json").content(promptGlobal(0, "Regra global")))
        .andExpect(status().isForbidden());
    jdbc.update("update usuarios set superusuario=false where id=?", user);
    for (String rota : List.of("prompt-global", "prompt-global/historico", "metricas", "prompt-global/previa/" + own)) {
      mvc.perform(get("/api/superusuario/" + rota).cookie(session)).andExpect(status().isForbidden());
    }
    assertThat(jdbc.queryForObject("select revisao from configuracoes_globais where id=1", Long.class)).isZero();
  }

  @Test
  void promptGlobalTemHistoricoPreviaEConflitoDeRevisao() throws Exception {
    jdbc.update("update usuarios set superusuario=true where id=?", user);
    mvc.perform(put("/api/superusuario/prompt-global").cookie(session).with(csrf())
        .contentType("application/json").content(promptGlobal(0, "Respeite as regras gerais.")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revisao").value(1))
        .andExpect(jsonPath("$.atualizadoPorEmail").value("owner@example.test"));
    mvc.perform(get("/api/superusuario/prompt-global").cookie(session))
        .andExpect(jsonPath("$.promptSistema").value("Respeite as regras gerais."));
    mvc.perform(get("/api/superusuario/prompt-global/previa/" + own).cookie(session))
        .andExpect(jsonPath("$.promptComposto").value(org.hamcrest.Matchers.startsWith("Respeite as regras gerais.")))
        .andExpect(jsonPath("$.promptComposto").value(org.hamcrest.Matchers.containsString("Empresa: Empresa de teste")));
    mvc.perform(put("/api/superusuario/prompt-global").cookie(session).with(csrf())
        .contentType("application/json").content(promptGlobal(0, "Versão obsoleta")))
        .andExpect(status().isConflict());
    mvc.perform(put("/api/superusuario/prompt-global").cookie(session).with(csrf())
        .contentType("application/json").content(promptGlobal(1, " ")))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/superusuario/prompt-global/historico").cookie(session))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].promptSistema").value("Respeite as regras gerais."));
    assertThat(jdbc.queryForObject("select revisao from configuracoes_globais where id=1", Long.class)).isEqualTo(1);
  }

  @Test
  void somenteUmUsuarioPodeTerPermissaoSuprema() {
    jdbc.update("update usuarios set superusuario=true where id=?", user);
    UUID segundoUsuario = UUID.randomUUID();
    jdbc.update("insert into usuarios(id,email,hash_senha) values(?,'outro@example.test','hash')", segundoUsuario);
    assertThatThrownBy(() -> jdbc.update("update usuarios set superusuario=true where id=?", segundoUsuario))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(jdbc.queryForObject("select count(*) from usuarios where superusuario", Long.class)).isEqualTo(1);
  }

  @Test
  void coletaTemCredencialSeparadaEEventosRepetidosNaoContamDuasVezes() throws Exception {
    String evento = eventoMensagem(own, "envio-1", "enviada", "sucesso", Instant.now().minusSeconds(20));
    mvc.perform(post("/api/integracoes/mensagens").contentType("application/json").content(evento))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/integracoes/mensagens").header("X-Token-Metricas", "errado")
        .contentType("application/json").content(evento)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/superusuario/prompt-global").header("X-Token-Metricas", "credencial-exclusiva-teste-2026"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/integracoes/mensagens").header("X-Token-Metricas", "credencial-exclusiva-teste-2026")
        .contentType("application/json").content(evento))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.registrado").value(true));
    mvc.perform(post("/api/integracoes/mensagens").header("X-Token-Metricas", "credencial-exclusiva-teste-2026")
        .contentType("application/json").content(evento))
        .andExpect(status().isOk()).andExpect(jsonPath("$.registrado").value(false));
    assertThat(jdbc.queryForObject("select count(*) from eventos_mensagens_atendimento", Long.class)).isEqualTo(1);
    assertThatThrownBy(() -> new ControladorIntegracaoMetricas(registroMensagens, "").registrar(
        "qualquer", new ServicoRegistroMensagens.EventoMensagem(own, "teste", "desabilitado", "enviada", "sucesso", Instant.now())))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("503");
  }

  @Test
  void metricasUsamJanelaDeTempoEErrosNaoSaoEnviosBemSucedidos() throws Exception {
    Instant agora = Instant.now();
    for (String evento : List.of(
        eventoMensagem(own, "recente", "enviada", "sucesso", agora.minusSeconds(20)),
        eventoMensagem(own, "recebida", "recebida", "sucesso", agora.minusSeconds(3600)),
        eventoMensagem(other, "historica", "enviada", "sucesso", agora.minusSeconds(172800)),
        eventoMensagem(own, "falhou", "enviada", "erro", agora.minusSeconds(20)))) {
      mvc.perform(post("/api/integracoes/mensagens").header("X-Token-Metricas", "credencial-exclusiva-teste-2026")
          .contentType("application/json").content(evento)).andExpect(status().isCreated());
    }
    mvc.perform(post("/api/integracoes/mensagens").header("X-Token-Metricas", "credencial-exclusiva-teste-2026")
        .contentType("application/json").content(eventoMensagem(own, "futuro", "enviada", "sucesso", agora.plusSeconds(86400))))
        .andExpect(status().isBadRequest());
    jdbc.update("update usuarios set superusuario=true where id=?", user);
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
    assertThat(jdbc.queryForObject("select count(*) from eventos_mensagens_atendimento", Long.class)).isEqualTo(4);
  }

  private String promptGlobal(long revisao, String texto) {
    return "{\"promptSistema\":\"" + texto + "\",\"complementarConfiguracoesEmpresa\":true,\"revisao\":" + revisao + "}";
  }

  private String eventoMensagem(UUID empresaId, String eventoId, String direcao, String resultado, Instant ocorridoEm) {
    return "{\"empresaId\":\"" + empresaId + "\",\"origem\":\"teste\",\"idEventoOrigem\":\"" + eventoId
        + "\",\"direcao\":\"" + direcao + "\",\"resultado\":\"" + resultado + "\",\"ocorridoEm\":\"" + ocorridoEm + "\"}";
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
