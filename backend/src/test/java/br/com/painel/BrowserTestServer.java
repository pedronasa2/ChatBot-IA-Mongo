package br.com.painel;

import java.nio.file.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Banco descartável e usuário exclusivo dos testes de navegador. Nunca usar em produção. */
public class BrowserTestServer {
  public static void main(String[] args) throws Exception {
    String porta = System.getenv().getOrDefault("E2E_BACKEND_PORT", "8087");
    var app = new SpringApplication(Aplicacao.class);
    var context =
        app.run(
            "--spring.mongodb.uri=" + MongoFixture.URI,
            "--server.servlet.session.cookie.secure=false",
            "--server.address=127.0.0.1",
            "--server.port=" + porta,
            "--painel.integracao.token-modulos="
                + System.getenv().getOrDefault("E2E_MODULES_TOKEN", ""),
            "--painel.integracao.token-metricas="
                + System.getenv().getOrDefault("E2E_METRICS_TOKEN", ""));
    var mongo = context.getBean(MongoTemplate.class);
    String password = UUID.randomUUID().toString();
    UUID user = UUID.randomUUID(), empresa = UUID.randomUUID();
    var encoder = context.getBean(PasswordEncoder.class);
    MongoFixture.usuario(mongo, user, "browser@example.test", encoder.encode(password), false);
    MongoFixture.usuario(
        mongo, UUID.randomUUID(), "developer@example.test", encoder.encode(password), true);
    MongoFixture.empresa(mongo, empresa, user);
    MongoFixture.vinculo(mongo, user, empresa);
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
                    mongo.getDb().drop();
                  } catch (Exception ignored) {
                  }
                }));
  }
}
