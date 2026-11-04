package br.com.painel.core.configuration;

import br.com.painel.core.service.MigracaoModelagemService;

import org.bson.Document;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.*;

import java.util.List;

@Configuration
public class ConfiguracaoMongo {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }

    @Bean
    @org.springframework.core.annotation.Order(0)
    ApplicationRunner prepararMongo(MongoTemplate mongo, MigracaoModelagemService migracao) {
        return args -> {
            for (var colecao :
                    List.of("usuarios", "empresas", "configuracoes_sistema", "eventos_mensagens")) {
                if (!mongo.collectionExists(colecao)) {
                    mongo.createCollection(colecao);
                }
            }
            migracao.converter();
            mongo.indexOps("usuarios")
                    .ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
            mongo.indexOps("usuarios").ensureIndex(new Index().on("empresaId", Sort.Direction.ASC));
            mongo.indexOps("usuarios")
                    .ensureIndex(
                            new Index()
                                    .on("convite.tokenHash", Sort.Direction.ASC)
                                    .unique()
                                    .partial(
                                            PartialIndexFilter.of(
                                                    new Document(
                                                            "convite.tokenHash",
                                                            new Document("$type", "string")))));
            mongo.indexOps("empresas")
                    .ensureIndex(
                            new Index()
                                    .on("chatwootAccountId", Sort.Direction.ASC)
                                    .on("chatwootInboxId", Sort.Direction.ASC)
                                    .unique()
                                    .partial(
                                            PartialIndexFilter.of(
                                                    new Document(
                                                                    "chatwootAccountId",
                                                                    new Document("$type", "number"))
                                                            .append(
                                                                    "chatwootInboxId",
                                                                    new Document(
                                                                            "$type", "number")))));
            mongo.indexOps("eventos_mensagens")
                    .ensureIndex(
                            new Index()
                                    .on("empresaId", Sort.Direction.ASC)
                                    .on("enviadoEm", Sort.Direction.ASC));
            mongo.indexOps("eventos_mensagens")
                    .ensureIndex(new Index().on("enviadoEm", Sort.Direction.ASC));
            migracao.removerLegadas();
            // Remove indexes on fields that no longer exist, keeping session infrastructure
            // untouched.
            for (var colecao : List.of("usuarios", "empresas")) {
                for (var indice : mongo.getCollection(colecao).listIndexes()) {
                    var campos = indice.get("key", Document.class).keySet();
                    if (campos.stream().anyMatch(c -> c.contains("_") && !c.equals("_id"))
                            || campos.contains("superusuario")) {
                        mongo.indexOps(colecao).dropIndex(indice.getString("name"));
                    }
                }
            }
        };
    }
}
