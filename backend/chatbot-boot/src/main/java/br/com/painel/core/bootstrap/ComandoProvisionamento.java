package br.com.painel.core.bootstrap;

import br.com.painel.autenticacao.service.ProvisionamentoService;

import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
@Profile("provision")
public class ComandoProvisionamento implements ApplicationRunner {
    private final PasswordEncoder encoder;
    private final ProvisionamentoService servico;

    public ComandoProvisionamento(PasswordEncoder encoder, ProvisionamentoService servico) {
        this.encoder = encoder;
        this.servico = servico;
    }

    public void run(ApplicationArguments args) {
        var console = System.console();
        if (console == null) {
            throw new IllegalStateException("Execute provisionamento em um terminal interativo.");
        }
        String email =
                console.readLine("E-mail do primeiro usuário: ").strip().toLowerCase(Locale.ROOT);
        String nome = console.readLine("Nome da empresa: ").strip();
        char[] password = console.readPassword("Senha (mínimo 12 caracteres): ");
        char[] confirmation = console.readPassword("Confirme a senha: ");
        try {
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                    || email.length() > 254
                    || nome.isBlank()
                    || nome.length() > 160
                    || password.length < 12
                    || new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                            > 72
                    || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException(
                        "Revise os dados e a confirmação de senha (12–72 caracteres).");
            }
            String hash = encoder.encode(new String(password));
            servico.criar(email, nome, hash);
            console.printf(
                    "Usuário e empresa criados. Bot pausado, rascunho inicial disponível.%n");
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
