package br.com.painel.autenticacao.repository;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

/** Atomic initial installation only; never an HTTP management API. */
@Repository
public class ProvisionamentoRepository {
    private final MongoTemplate mongo;

    public ProvisionamentoRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void criar(String email, String nome, String hash, UUID u, UUID c) {
        mongo.insert(new Document("_id", "inicial"), "provisionamento");
        if (mongo.count(new Query(), "usuarios") != 0) {
            throw new IllegalStateException("Provisionamento inicial já realizado.");
        }
        var agora = new Date();
        mongo.insert(
                new Document("_id", u)
                        .append("email", email)
                        .append("hash_senha", hash)
                        .append("ativo", true)
                        .append("superusuario", true)
                        .append("criado_em", agora)
                        .append("atualizado_em", agora),
                "usuarios");
        mongo.insert(
                new Document("_id", c)
                        .append("nome", nome)
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
                        .append("criado_em", agora)
                        .append("atualizado_em", agora),
                "empresas");
        mongo.insert(
                new Document("_id", UUID.randomUUID())
                        .append("usuario_id", u)
                        .append("empresa_id", c)
                        .append("papel", "proprietario"),
                "vinculos_usuarios_empresas");
        mongo.insert(
                new Document("_id", UUID.randomUUID())
                        .append("empresa_id", c)
                        .append("versao", 1)
                        .append("estado", "rascunho")
                        .append("autor_id", u)
                        .append("nome_bot", "Assistente")
                        .append("tom_voz", "cordial")
                        .append("usar_emojis", false)
                        .append("instrucoes", "")
                        .append("instrucoes_transferencia", "")
                        .append("revisao", 0L)
                        .append("criado_em", agora)
                        .append("atualizado_em", agora),
                "versoes_configuracoes_bot");
    }
}
