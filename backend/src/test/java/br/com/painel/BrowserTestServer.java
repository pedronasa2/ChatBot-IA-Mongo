package br.com.painel;

import java.nio.file.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Banco descartável e usuário exclusivo dos testes de navegador. Nunca usar em produção. */
public class BrowserTestServer {
  public static void main(String[] args) throws Exception {
    String porta = System.getenv().getOrDefault("E2E_BACKEND_PORT", "8087");
    var app = new SpringApplication(Aplicacao.class);
    app.setDefaultProperties(
        Map.of(
            "spring.datasource.url",
            PostgresFixture.DB.getJdbcUrl("postgres", "postgres"),
            "spring.datasource.username",
            "postgres",
            "spring.datasource.password",
            "",
            "server.servlet.session.cookie.secure",
            "false",
            "server.address",
            "127.0.0.1",
            "server.port",
            porta));
    var context =
        app.run(
            "--spring.datasource.url=" + PostgresFixture.DB.getJdbcUrl("postgres", "postgres"),
            "--spring.datasource.username=postgres",
            "--spring.datasource.password=",
            "--server.servlet.session.cookie.secure=false",
            "--server.address=127.0.0.1",
            "--server.port=" + porta,
            "--painel.integracao.token-metricas=" + System.getenv().getOrDefault("E2E_METRICS_TOKEN", ""));
    var jdbc = context.getBean(JdbcTemplate.class);
    String password = UUID.randomUUID().toString();
    UUID user = UUID.randomUUID(), empresa = UUID.randomUUID();
    jdbc.update(
        "insert into usuarios(id,email,hash_senha) values(?,?,?)",
        user,
        "browser@example.test",
        context.getBean(PasswordEncoder.class).encode(password));
    jdbc.update(
        "insert into usuarios(id,email,hash_senha,superusuario) values(?,'developer@example.test',?,true)",
        UUID.randomUUID(), context.getBean(PasswordEncoder.class).encode(password));
    jdbc.update("insert into empresas(id,nome) values(?,'Empresa de teste')", empresa);
    jdbc.update(
        "insert into vinculos_usuarios_empresas(id,usuario_id,empresa_id,papel) values(?,?,?,'proprietario')",
        UUID.randomUUID(),
        user,
        empresa);
    jdbc.update(
        "insert into versoes_configuracoes_bot(id,empresa_id,versao,estado,autor_id)"
            + " values(?,?,1,'rascunho',?)",
        UUID.randomUUID(),
        empresa,
        user);
    String file = System.getenv("E2E_CREDENTIAL_FILE");
    if (file == null)
      throw new IllegalStateException("Defina E2E_CREDENTIAL_FILE fora do repositório.");
    var path = Path.of(file);
    Files.writeString(path, password);
    Files.setPosixFilePermissions(
        path, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    Files.deleteIfExists(path);
                    PostgresFixture.DB.close();
                  } catch (Exception ignored) {
                  }
                }));
  }
}
