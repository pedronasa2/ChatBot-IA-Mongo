package br.com.painel.persistencia;

import java.time.Instant;
import java.util.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Component;

@Component
public class Documentos {
  public final MongoTemplate mongo;

  public Documentos(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  public static Query q(String campo, Object valor) {
    return Query.query(Criteria.where(campo).is(valor));
  }

  public Document um(String colecao, Query q) {
    return mongo.findOne(q, Document.class, colecao);
  }

  public List<Document> lista(String colecao, Query q) {
    return mongo.find(q, Document.class, colecao);
  }

  public long contar(String colecao, Query q) {
    return mongo.count(q, colecao);
  }

  public void inserir(String colecao, Document d) {
    mongo.insert(d, colecao);
  }

  public static Instant instante(Document d, String chave) {
    Date v = d.getDate(chave);
    return v == null ? null : v.toInstant();
  }

  public static long numero(Document d, String chave) {
    return ((Number) d.getOrDefault(chave, 0L)).longValue();
  }

  public void auditar(UUID empresa, UUID usuario, UUID config, String acao) {
    inserir(
        "eventos_auditoria",
        new Document("_id", UUID.randomUUID())
            .append("empresa_id", empresa)
            .append("usuario_responsavel_id", usuario)
            .append("configuracao_bot_id", config)
            .append("acao", acao)
            .append("criado_em", new Date()));
  }
}
