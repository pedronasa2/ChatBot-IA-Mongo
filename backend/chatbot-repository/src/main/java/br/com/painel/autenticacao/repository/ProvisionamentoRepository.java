package br.com.painel.autenticacao.repository;

import br.com.painel.autenticacao.model.*;
import br.com.painel.empresas.model.Empresa;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class ProvisionamentoRepository {

    private final MongoTemplate mongo;

    public ProvisionamentoRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void criar(String email, String nome, String hash, UUID usuarioId, UUID empresaId) {
        if (mongo.count(new Query(), Usuario.class) != 0) {
            throw new IllegalStateException("Provisionamento inicial já realizado.");
        }
        var agora = Instant.now();
        var u = new Usuario();
        u.id = usuarioId;
        u.nome = nome;
        u.email = email;
        u.hashSenha = hash;
        u.perfil = PerfilUsuario.SUPERUSUARIO;
        u.ativo = true;
        u.criadoEm = agora;
        u.atualizadoEm = agora;
        mongo.insert(u);
        var e = new Empresa();
        e.id = empresaId;
        e.nome = nome;
        e.criadoEm = agora;
        e.atualizadoEm = agora;
        mongo.insert(e);
    }
}
