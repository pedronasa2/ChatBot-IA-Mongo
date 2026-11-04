package br.com.painel;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import br.com.painel.autenticacao.model.*;
import br.com.painel.empresas.model.Empresa;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.UUID;

public class MongoFixture {

    public static final String DATABASE =
            "teste_painel_" + UUID.randomUUID().toString().replace("-", "");

    public static final String URI =
            "mongodb://localhost:27027/"
                    + DATABASE
                    + "?replicaSet=rs0&directConnection=true&uuidRepresentation=standard";

    public static void usuario(
            MongoTemplate m, UUID id, String email, String senha, boolean admin) {
        var u = new Usuario();
        u.id = id;
        u.nome = email;
        u.email = email;
        u.hashSenha = senha;
        u.perfil = admin ? PerfilUsuario.SUPERUSUARIO : PerfilUsuario.USUARIO;
        u.ativo = true;
        u.criadoEm = Instant.now();
        u.atualizadoEm = Instant.now();
        m.insert(u);
    }

    public static void vinculo(MongoTemplate m, UUID usuario, UUID empresa) {
        m.updateFirst(
                consultaPor("_id", usuario), new Update().set("empresaId", empresa), Usuario.class);
    }

    public static void empresa(MongoTemplate m, UUID id, UUID usuario) {
        var e = new Empresa();
        e.id = id;
        e.nome = "Empresa " + id.toString().substring(0, 8);
        e.bot.usarEmojis = true;
        e.criadoEm = Instant.now();
        e.atualizadoEm = Instant.now();
        m.insert(e);
    }
}
