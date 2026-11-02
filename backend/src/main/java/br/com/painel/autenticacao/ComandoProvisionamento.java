package br.com.painel.autenticacao;

import br.com.painel.persistencia.Documentos;
import java.util.*;
import org.bson.Document;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
@Profile("provision")
public class ComandoProvisionamento implements ApplicationRunner {
  private final Documentos documentos;
  private final PasswordEncoder encoder;
  private final TransactionTemplate tx;

  public ComandoProvisionamento(
      Documentos j,
      PasswordEncoder e,
      org.springframework.transaction.PlatformTransactionManager m) {
    documentos = j;
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
            documentos.inserir("provisionamento", new Document("_id", "inicial"));
            if (documentos.contar("usuarios", new Query()) != 0)
              throw new IllegalStateException("Provisionamento inicial já realizado.");
            var agora = new Date();
            documentos.inserir(
                "usuarios",
                new Document("_id", u)
                    .append("email", email)
                    .append("hash_senha", hash)
                    .append("ativo", true)
                    .append("superusuario", true)
                    .append("criado_em", agora)
                    .append("atualizado_em", agora));
            documentos.inserir(
                "empresas",
                new Document("_id", c)
                    .append("nome", nome)
                    .append("descricao", "")
                    .append("endereco", "")
                    .append("contato", "")
                    .append("site", "")
                    .append("servicos", "")
                    .append("fuso_horario", "America/Sao_Paulo")
                    .append("bot_ativo", false)
                    .append("modulo_candidatos_habilitado", false)
                    .append("modulo_agenda_habilitado", false)
                    .append("revisao", 0L)
                    .append("criado_em", agora)
                    .append("atualizado_em", agora));
            documentos.inserir(
                "vinculos_usuarios_empresas",
                new Document("_id", UUID.randomUUID())
                    .append("usuario_id", u)
                    .append("empresa_id", c)
                    .append("papel", "proprietario"));
            documentos.inserir(
                "versoes_configuracoes_bot",
                new Document("_id", UUID.randomUUID())
                    .append("empresa_id", c)
                    .append("versao", 1)
                    .append("estado", "rascunho")
                    .append("autor_id", u)
                    .append("nome_bot", "Assistente")
                    .append("tom_voz", "cordial")
                    .append("usar_emojis", false)
                    .append("instrucoes", "")
                    .append("instrucoes_transferencia", "")
                    .append("revisao", 0L)
                    .append("criado_em", agora)
                    .append("atualizado_em", agora));
            documentos.auditar(c, u, null, "provisionamento_inicial");
          });
      console.printf("Usuário e empresa criados. Bot pausado, rascunho inicial disponível.%n");
    } finally {
      Arrays.fill(password, '\0');
      Arrays.fill(confirmation, '\0');
    }
  }
}
