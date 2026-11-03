package br.com.painel.candidatos.repository;

import static br.com.painel.core.repository.util.ValoresMongo.*;

import br.com.painel.candidatos.model.vo.Candidato;
import br.com.painel.candidatos.model.vo.DadosCandidato;
import br.com.painel.core.model.PaginaRegistros;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;

import jakarta.validation.constraints.*;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.*;

import java.util.*;

@org.springframework.stereotype.Repository
public class CandidatoRepository {

    private final org.springframework.data.mongodb.core.MongoTemplate mongo;

    public CandidatoRepository(org.springframework.data.mongodb.core.MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public PaginaRegistros<Candidato> listar(
            UUID empresaId, String busca, boolean incluirArquivados, int pagina, int tamanho) {
        Query consulta = consultaPor("empresa_id", empresaId);
        if (!incluirArquivados) {
            consulta.addCriteria(Criteria.where("arquivado").is(false));
        }
        String termo = busca == null ? "" : busca.strip();
        if (!termo.isEmpty()) {
            consulta.addCriteria(
                    new Criteria()
                            .orOperator(
                                    Criteria.where("nome_completo")
                                            .regex(java.util.regex.Pattern.quote(termo), "i"),
                                    Criteria.where("email")
                                            .regex(java.util.regex.Pattern.quote(termo), "i"),
                                    Criteria.where("cargo_pretendido")
                                            .regex(java.util.regex.Pattern.quote(termo), "i")));
        }
        long total = mongo.count(consulta, "candidatos");
        var registros =
                mongo
                        .find(
                                consulta.with(
                                                Sort.by(
                                                        Sort.Order.desc("recebido_em"),
                                                        Sort.Order.asc("_id")))
                                        .skip((long) pagina * tamanho)
                                        .limit(tamanho),
                                Document.class,
                                "candidatos")
                        .stream()
                        .map(this::mapear)
                        .toList();
        return new PaginaRegistros<>(registros, total, pagina, tamanho);
    }

    public Candidato consultar(UUID empresaId, UUID id) {
        return encontrar(empresaId, id);
    }

    private Document valores(DadosCandidato dados) {
        return new Document()
                .append("nome_completo", dados.nomeCompleto().strip())
                .append("email", dados.email().strip())
                .append("telefone", dados.telefone().strip())
                .append("cargo_pretendido", dados.cargoPretendido().strip())
                .append("url_curriculo", dados.urlCurriculo().strip())
                .append("resumo_profissional", dados.resumoProfissional())
                .append("observacoes", dados.observacoes())
                .append("situacao", dados.situacao())
                .append("arquivado", dados.arquivado())
                .append("atualizado_em", new Date());
    }

    private Update alteracoes(DadosCandidato dados) {
        Update update = new Update();
        valores(dados).forEach(update::set);
        return update;
    }

    public Candidato criar(UUID empresaId, DadosCandidato dados) {
        UUID id = UUID.randomUUID();
        Document documentoNovo =
                new Document("_id", id)
                        .append("empresa_id", empresaId)
                        .append("revisao", 0L)
                        .append("recebido_em", new Date());
        documentoNovo.putAll(valores(dados));
        mongo.insert(documentoNovo, "candidatos");
        return mapear(documentoNovo);
    }

    public Candidato atualizar(UUID empresaId, UUID id, DadosCandidato dados) {
        encontrar(empresaId, id);
        var resultado =
                mongo.updateFirst(
                        consultaPor("_id", id)
                                .addCriteria(Criteria.where("empresa_id").is(empresaId))
                                .addCriteria(Criteria.where("revisao").is(dados.revisao())),
                        alteracoes(dados).inc("revisao", 1),
                        "candidatos");
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
                                "candidatos")
                        .getModifiedCount());
    }

    private Candidato encontrar(UUID empresaId, UUID id) {
        var documentoNovo =
                mongo.findOne(
                        consultaPor("_id", id)
                                .addCriteria(Criteria.where("empresa_id").is(empresaId)),
                        Document.class,
                        "candidatos");
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

    private Candidato mapear(Document documento) {
        return new Candidato(
                documento.get("_id", UUID.class),
                documento.get("empresa_id", UUID.class),
                documento.getString("nome_completo"),
                documento.getString("email"),
                documento.getString("telefone"),
                documento.getString("cargo_pretendido"),
                documento.getString("url_curriculo"),
                documento.getString("resumo_profissional"),
                documento.getString("observacoes"),
                documento.getString("situacao"),
                documento.getBoolean("arquivado", false),
                numero(documento, "revisao"),
                instante(documento, "recebido_em"),
                instante(documento, "atualizado_em"));
    }
}
