package br.com.painel.auditoria.repository;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class AuditoriaRepository {
    private final MongoTemplate mongo;

    public AuditoriaRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void auditar(UUID empresa, UUID usuario, UUID config, String acao) {
        mongo.insert(
                new Document("_id", UUID.randomUUID())
                        .append("empresa_id", empresa)
                        .append("usuario_responsavel_id", usuario)
                        .append("configuracao_bot_id", config)
                        .append("acao", acao)
                        .append("criado_em", new Date()),
                "eventos_auditoria");
    }
}
