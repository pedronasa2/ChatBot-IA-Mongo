package br.com.painel.convites.repository;

import br.com.painel.convites.model.vo.NovaEmpresa;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/** Persistence of the initial company aggregate. Joins the invitation use case transaction. */
@Repository
public class CadastroEmpresaRepository {
    private final MongoTemplate mongo;

    public CadastroEmpresaRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private void inserir(String colecao, Document documento) {
        mongo.insert(documento, colecao);
    }

    public void criar(
            NovaEmpresa dados,
            String email,
            String senhaInutilizavel,
            UUID empresa,
            UUID usuario,
            UUID autor) {
        Date agora = new Date();
        inserir(
                "usuarios",
                new Document("_id", usuario)
                        .append("email", email)
                        .append("nome", dados.responsavel().strip())
                        .append("hash_senha", senhaInutilizavel)
                        .append("ativo", false)
                        .append("superusuario", false)
                        .append("criado_em", agora)
                        .append("atualizado_em", agora));
        inserir(
                "empresas",
                new Document("_id", empresa)
                        .append("nome", dados.nome().strip())
                        .append("descricao", "")
                        .append("endereco", "")
                        .append("contato", "")
                        .append("site", "")
                        .append("servicos", "")
                        .append("fuso_horario", "America/Sao_Paulo")
                        .append("bot_ativo", false)
                        .append("modulo_candidatos_habilitado", dados.moduloCandidatosHabilitado())
                        .append("modulo_agenda_habilitado", dados.moduloAgendaHabilitado())
                        .append("revisao", 0L)
                        .append("criado_em", agora)
                        .append("atualizado_em", agora));
        inserir(
                "vinculos_usuarios_empresas",
                new Document("_id", UUID.randomUUID())
                        .append("usuario_id", usuario)
                        .append("empresa_id", empresa)
                        .append("papel", "proprietario")
                        .append("criado_em", agora));
        inserir(
                "versoes_configuracoes_bot",
                new Document("_id", UUID.randomUUID())
                        .append("empresa_id", empresa)
                        .append("versao", 1)
                        .append("estado", "rascunho")
                        .append("autor_id", autor)
                        .append("nome_bot", "Assistente")
                        .append("tom_voz", "cordial")
                        .append("usar_emojis", false)
                        .append("instrucoes", "")
                        .append("instrucoes_transferencia", "")
                        .append("revisao", 0L)
                        .append("criado_em", agora)
                        .append("atualizado_em", agora));
    }
}
