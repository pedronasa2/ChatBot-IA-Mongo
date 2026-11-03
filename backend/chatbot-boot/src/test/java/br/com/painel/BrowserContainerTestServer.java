package br.com.painel;

import com.mongodb.client.MongoClients;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.*;
import java.util.*;

/**
 * Runs the production image against the same disposable fixture as the classpath browser server.
 */
public class BrowserContainerTestServer {
    public static void main(String[] args) throws Exception {
        String image = Objects.requireNonNull(System.getenv("E2E_IMAGE"));
        String port = System.getenv().getOrDefault("E2E_BACKEND_PORT", "8087");
        Path credential = Path.of(Objects.requireNonNull(System.getenv("E2E_CREDENTIAL_FILE")));
        String name = "chatbot-browser-" + UUID.randomUUID();
        var client = MongoClients.create(MongoFixture.URI);
        var mongo = new MongoTemplate(client, MongoFixture.DATABASE);
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    try {
                                        new ProcessBuilder("docker", "stop", "-t", "3", name)
                                                .redirectErrorStream(true)
                                                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                                .start()
                                                .waitFor();
                                        mongo.getDb().drop();
                                        Files.deleteIfExists(credential);
                                    } catch (Exception ignored) {
                                    } finally {
                                        client.close();
                                    }
                                }));
        String password = UUID.randomUUID().toString();
        var encoder = new BCryptPasswordEncoder(12);
        UUID user = UUID.randomUUID(), empresa = UUID.randomUUID();
        MongoFixture.usuario(mongo, user, "browser@example.test", encoder.encode(password), false);
        MongoFixture.usuario(
                mongo, UUID.randomUUID(), "developer@example.test", encoder.encode(password), true);
        MongoFixture.empresa(mongo, empresa, user);
        MongoFixture.vinculo(mongo, user, empresa);
        Files.writeString(credential, password);
        Files.setPosixFilePermissions(
                credential, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        Process container =
                new ProcessBuilder(
                                "docker",
                                "run",
                                "--rm",
                                "--network",
                                "host",
                                "--name",
                                name,
                                "-e",
                                "MONGODB_URI=" + MongoFixture.URI,
                                "-e",
                                "SERVER_ADDRESS=127.0.0.1",
                                "-e",
                                "SERVER_PORT=" + port,
                                "-e",
                                "COOKIE_SECURE=false",
                                "-e",
                                "MODULES_INTEGRATION_TOKEN="
                                        + System.getenv().getOrDefault("E2E_MODULES_TOKEN", ""),
                                "-e",
                                "METRICS_INGESTION_TOKEN="
                                        + System.getenv().getOrDefault("E2E_METRICS_TOKEN", ""),
                                image)
                        .inheritIO()
                        .start();
        int exit = container.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("A imagem de teste terminou com código " + exit);
        }
    }
}
