package br.com.painel.convites.repository;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import br.com.painel.convites.model.ConviteEmpresa;

import org.springframework.data.mongodb.core.*;
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

    public ConviteEmpresa buscar(UUID empresa) {
        return mongo.findOne(consultaPor("empresa_id", empresa), ConviteEmpresa.class);
    }

    public void renovar(UUID empresa, UUID usuario, String email, String hash, Instant expira) {
        mongo.upsert(
                consultaPor("empresa_id", empresa),
                new Update()
                        .set("usuario_id", usuario)
                        .set("email", email)
                        .set("token_hash", hash)
                        .set("expira_em", Date.from(expira))
                        .set("usado_em", null)
                        .set("criado_em", new Date()),
                "convites_empresas");
    }

    public ConviteEmpresa consumir(String hash, String email) {
        return mongo.findAndModify(
                consultaPor("token_hash", hash)
                        .addCriteria(Criteria.where("email").is(email))
                        .addCriteria(Criteria.where("usado_em").is(null))
                        .addCriteria(Criteria.where("expira_em").gt(new Date())),
                new Update().set("usado_em", new Date()),
                FindAndModifyOptions.options().returnNew(true),
                ConviteEmpresa.class);
    }
}
