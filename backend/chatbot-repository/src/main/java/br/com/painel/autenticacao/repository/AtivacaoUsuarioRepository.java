package br.com.painel.autenticacao.repository;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class AtivacaoUsuarioRepository {
    private final MongoTemplate mongo;

    public AtivacaoUsuarioRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public boolean ativar(UUID usuario, String hashSenha) {
        return mongo.updateFirst(
                                consultaPor("_id", usuario)
                                        .addCriteria(Criteria.where("ativo").is(false)),
                                new Update()
                                        .set("hash_senha", hashSenha)
                                        .set("ativo", true)
                                        .set("atualizado_em", new Date()),
                                "usuarios")
                        .getModifiedCount()
                == 1;
    }
}
