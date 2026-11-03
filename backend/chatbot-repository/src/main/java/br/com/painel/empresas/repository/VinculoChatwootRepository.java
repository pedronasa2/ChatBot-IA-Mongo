package br.com.painel.empresas.repository;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import br.com.painel.superusuario.model.vo.DadosVinculoChatwoot;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class VinculoChatwootRepository {
    private final MongoTemplate mongo;

    public VinculoChatwootRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void salvar(UUID empresa, DadosVinculoChatwoot dados, UUID autor) {
        mongo.updateFirst(
                consultaPor("_id", empresa),
                new Update()
                        .set("chatwoot_account_id", dados.accountId())
                        .set("chatwoot_inbox_id", dados.inboxId())
                        .inc("revisao", 1)
                        .set("atualizado_em", new Date())
                        .set("atualizado_por", autor),
                "empresas");
    }
}
