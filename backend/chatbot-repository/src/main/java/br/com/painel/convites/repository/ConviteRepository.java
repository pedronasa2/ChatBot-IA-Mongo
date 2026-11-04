package br.com.painel.convites.repository;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import br.com.painel.autenticacao.model.Usuario;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
public class ConviteRepository {

    private final MongoTemplate mongo;

    public ConviteRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Usuario buscar(UUID empresa) {
        return mongo.findOne(
                consultaPor("empresaId", empresa).addCriteria(Criteria.where("convite").ne(null)),
                Usuario.class);
    }

    public boolean renovar(UUID usuario, String hash, Instant expira) {
        return mongo.updateFirst(
                                consultaPor("_id", usuario)
                                        .addCriteria(Criteria.where("ativo").is(false))
                                        .addCriteria(Criteria.where("convite.usadoEm").is(null)),
                                new Update()
                                        .set("convite.tokenHash", hash)
                                        .set("convite.expiraEm", expira)
                                        .set("convite.usadoEm", null)
                                        .set("atualizadoEm", Instant.now()),
                                Usuario.class)
                        .getModifiedCount()
                == 1;
    }

    public boolean ativar(String hash, String email, String hashSenha) {
        return mongo.updateFirst(
                                consultaPor("convite.tokenHash", hash)
                                        .addCriteria(Criteria.where("email").is(email))
                                        .addCriteria(Criteria.where("ativo").is(false))
                                        .addCriteria(Criteria.where("convite.usadoEm").is(null))
                                        .addCriteria(
                                                Criteria.where("convite.expiraEm")
                                                        .gt(Instant.now())),
                                new Update()
                                        .set("convite.usadoEm", Instant.now())
                                        .set("ativo", true)
                                        .set("hashSenha", hashSenha)
                                        .set("atualizadoEm", Instant.now()),
                                Usuario.class)
                        .getModifiedCount()
                == 1;
    }
}
