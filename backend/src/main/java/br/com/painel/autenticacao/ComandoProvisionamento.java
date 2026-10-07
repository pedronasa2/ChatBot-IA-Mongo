package br.com.painel.autenticacao;

import java.util.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("provision")
public class ComandoProvisionamento implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final TransactionTemplate tx;

  public ComandoProvisionamento(
      JdbcTemplate j,
      PasswordEncoder e,
      org.springframework.transaction.PlatformTransactionManager m) {
    jdbc = j;
    encoder = e;
    tx = new TransactionTemplate(m);
  }

  public void run(ApplicationArguments args) {
    var console = System.console();
    if (console == null)
      throw new IllegalStateException("Execute provisionamento em um terminal interativo.");
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
          || new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
          || !Arrays.equals(password, confirmation))
        throw new IllegalArgumentException(
            "Revise os dados e a confirmação de senha (12–72 caracteres).");
      String hash = encoder.encode(new String(password));
      UUID u = UUID.randomUUID(), c = UUID.randomUUID();
      tx.executeWithoutResult(
          s -> {
            jdbc.execute("LOCK TABLE usuarios IN EXCLUSIVE MODE");
            if (jdbc.queryForObject("select count(*) from usuarios", Long.class) != 0)
              throw new IllegalStateException("Provisionamento inicial já realizado.");
            jdbc.update("insert into usuarios(id,email,hash_senha) values(?,?,?)", u, email, hash);
            jdbc.update("insert into empresas(id,nome) values(?,?)", c, nome);
            jdbc.update(
                "insert into vinculos_usuarios_empresas(id,usuario_id,empresa_id,papel) values(?,?,?,'proprietario')",
                UUID.randomUUID(),
                u,
                c);
            jdbc.update(
                "insert into versoes_configuracoes_bot(id,empresa_id,versao,estado,autor_id)"
                    + " values(?,?,1,'rascunho',?)",
                UUID.randomUUID(),
                c,
                u);
            jdbc.update(
                "insert into eventos_auditoria(id,empresa_id,usuario_responsavel_id,acao)"
                    + " values(?,?,?,'provisionamento_inicial')",
                UUID.randomUUID(),
                c,
                u);
          });
      console.printf("Usuário e empresa criados. Bot pausado, rascunho inicial disponível.%n");
    } finally {
      Arrays.fill(password, '\0');
      Arrays.fill(confirmation, '\0');
    }
  }
}
