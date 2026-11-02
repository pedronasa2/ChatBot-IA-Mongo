package br.com.painel;

import java.util.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Banco aleatório exclusivo dos testes. Nunca aponta ao banco da aplicação. */
public class MongoFixture {
  public static final String DATABASE =
      "teste_painel_" + UUID.randomUUID().toString().replace("-", "");
  public static final String URI =
      "mongodb://localhost:27027/"
          + DATABASE
          + "?replicaSet=rs0&directConnection=true&uuidRepresentation=standard";

  public static void global(MongoTemplate m) {
    m.insert(
        new Document("_id", 1)
            .append("prompt_sistema", "")
            .append("complementar_configuracoes_empresa", true)
            .append("revisao", 0L)
            .append("atualizado_em", new Date()),
        "configuracoes_globais");
  }

  public static void usuario(MongoTemplate m, UUID id, String email, String senha, boolean admin) {
    m.insert(
        new Document("_id", id)
            .append("email", email)
            .append("hash_senha", senha)
            .append("ativo", true)
            .append("superusuario", admin),
        "usuarios");
  }

  public static void vinculo(MongoTemplate m, UUID user, UUID empresa) {
    m.insert(
        new Document("_id", UUID.randomUUID())
            .append("usuario_id", user)
            .append("empresa_id", empresa)
            .append("papel", "proprietario"),
        "vinculos_usuarios_empresas");
  }

  public static void empresa(MongoTemplate m, UUID id, UUID user) {
    m.insert(
        new Document("_id", id)
            .append("nome", "Empresa de teste")
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
            .append("criado_em", new Date())
            .append("atualizado_em", new Date()),
        "empresas");
    m.insert(
        new Document("_id", UUID.randomUUID())
            .append("empresa_id", id)
            .append("versao", 1)
            .append("estado", "rascunho")
            .append("autor_id", user)
            .append("nome_bot", "Assistente")
            .append("tom_voz", "cordial")
            .append("usar_emojis", true)
            .append("instrucoes", "")
            .append("instrucoes_transferencia", "")
            .append("revisao", 0L)
            .append("criado_em", new Date())
            .append("atualizado_em", new Date()),
        "versoes_configuracoes_bot");
  }
}
