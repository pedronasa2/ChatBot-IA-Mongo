package br.com.painel.agenda.repository;

import static br.com.painel.core.repository.util.ValoresMongo.*;

import br.com.painel.agenda.model.vo.Agendamento;
import br.com.painel.agenda.model.vo.DadosAgendamento;
import br.com.painel.core.model.PaginaRegistros;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;

import jakarta.validation.constraints.*;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.*;

import java.time.Instant;
import java.util.*;

@org.springframework.stereotype.Repository
public class AgendamentoRepository {

    private final org.springframework.data.mongodb.core.MongoTemplate mongo;

    public AgendamentoRepository(org.springframework.data.mongodb.core.MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public PaginaRegistros<Agendamento> listar(
            UUID empresaId,
            String busca,
            boolean incluirArquivados,
            Instant de,
            Instant ate,
            int pagina,
            int tamanho) {
        Query consulta = consultaPor("empresa_id", empresaId);
        if (!incluirArquivados) {
            consulta.addCriteria(Criteria.where("arquivado").is(false));
        }
        String termo = busca == null ? "" : busca.strip();
        if (!termo.isEmpty()) {
            consulta.addCriteria(
                    new Criteria()
                            .orOperator(
                                    Criteria.where("nome_pessoa")
                                            .regex(java.util.regex.Pattern.quote(termo), "i"),
                                    Criteria.where("assunto")
                                            .regex(java.util.regex.Pattern.quote(termo), "i"),
                                    Criteria.where("responsavel")
                                            .regex(java.util.regex.Pattern.quote(termo), "i")));
        }
        if (de != null) {
            consulta.addCriteria(Criteria.where("fim").gt(Date.from(de)));
        }
        if (ate != null) {
            consulta.addCriteria(Criteria.where("inicio").lt(Date.from(ate)));
        }
        long total = mongo.count(consulta, "agendamentos_atendimento");
        var registros =
                mongo
                        .find(
                                consulta.with(Sort.by("inicio", "_id"))
                                        .skip((long) pagina * tamanho)
                                        .limit(tamanho),
                                Document.class,
                                "agendamentos_atendimento")
                        .stream()
                        .map(this::mapear)
                        .toList();
        return new PaginaRegistros<>(registros, total, pagina, tamanho);
    }

    public Agendamento consultar(UUID empresaId, UUID id) {
        return encontrar(empresaId, id);
    }

    private Document valores(DadosAgendamento dados) {
        return new Document()
                .append("nome_pessoa", dados.nomePessoa().strip())
                .append("email", dados.email().strip())
                .append("telefone", dados.telefone().strip())
                .append("assunto", dados.assunto().strip())
                .append("responsavel", dados.responsavel().strip())
                .append("inicio", Date.from(dados.inicio()))
                .append("fim", Date.from(dados.fim()))
                .append("situacao", dados.situacao())
                .append("observacoes", dados.observacoes())
                .append("arquivado", dados.arquivado())
                .append("atualizado_em", new Date());
    }

    private Update alteracoes(DadosAgendamento dados) {
        Update update = new Update();
        valores(dados).forEach(update::set);
        return update;
    }

    public Agendamento criar(UUID empresaId, DadosAgendamento dados) {
        UUID id = UUID.randomUUID();
        Document documentoNovo =
                new Document("_id", id)
                        .append("empresa_id", empresaId)
                        .append("revisao", 0L)
                        .append("criado_em", new Date());
        documentoNovo.putAll(valores(dados));
        mongo.insert(documentoNovo, "agendamentos_atendimento");
        return mapear(documentoNovo);
    }

    public Agendamento atualizar(UUID empresaId, UUID id, DadosAgendamento dados) {
        encontrar(empresaId, id);
        var resultado =
                mongo.updateFirst(
                        consultaPor("_id", id)
                                .addCriteria(Criteria.where("empresa_id").is(empresaId))
                                .addCriteria(Criteria.where("revisao").is(dados.revisao())),
                        alteracoes(dados).inc("revisao", 1),
                        "agendamentos_atendimento");
        exigirRevisao(resultado.getModifiedCount());
        return encontrar(empresaId, id);
    }

    public void arquivar(UUID empresaId, UUID id, long revisao) {
        encontrar(empresaId, id);
        exigirRevisao(
                mongo.updateFirst(
                                consultaPor("_id", id)
                                        .addCriteria(Criteria.where("empresa_id").is(empresaId))
                                        .addCriteria(Criteria.where("revisao").is(revisao)),
                                new Update()
                                        .set("arquivado", true)
                                        .inc("revisao", 1)
                                        .set("atualizado_em", new Date()),
                                "agendamentos_atendimento")
                        .getModifiedCount());
    }

    private Agendamento encontrar(UUID empresaId, UUID id) {
        var documentoNovo =
                mongo.findOne(
                        consultaPor("_id", id)
                                .addCriteria(Criteria.where("empresa_id").is(empresaId)),
                        Document.class,
                        "agendamentos_atendimento");
        if (documentoNovo == null) {
            throw new NegocioException(ErroNegocio.NAO_ENCONTRADO, "Registro não encontrado.");
        }
        return mapear(documentoNovo);
    }

    private void exigirRevisao(long alterados) {
        if (alterados != 1) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "Registro alterado em outra sessão. Recarregue antes de salvar.");
        }
    }

    private Agendamento mapear(Document documento) {
        return new Agendamento(
                documento.get("_id", UUID.class),
                documento.get("empresa_id", UUID.class),
                documento.getString("nome_pessoa"),
                documento.getString("email"),
                documento.getString("telefone"),
                documento.getString("assunto"),
                documento.getString("responsavel"),
                instante(documento, "inicio"),
                instante(documento, "fim"),
                documento.getString("situacao"),
                documento.getString("observacoes"),
                documento.getBoolean("arquivado", false),
                numero(documento, "revisao"),
                instante(documento, "criado_em"),
                instante(documento, "atualizado_em"));
    }
}
