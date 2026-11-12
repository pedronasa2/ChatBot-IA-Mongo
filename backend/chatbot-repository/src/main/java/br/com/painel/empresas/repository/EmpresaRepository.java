package br.com.painel.empresas.repository;

import br.com.painel.empresas.model.Empresa;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.*;

public interface EmpresaRepository extends MongoRepository<Empresa, UUID> {
    List<Empresa> findByIdIn(Collection<UUID> ids, Sort sort);

    @Aggregation(pipeline = {
        "{ $match: { nome: ?0 } }",
        """
        { $lookup: {
            from: 'eventos_mensagens',
            localField: '_id',
            foreignField: 'empresaId',
            as: 'mensagens'
        } }
        """,
        "{ $unwind: '$mensagens' }",
        "{ $match: { 'mensagens.enviadoEm': { $gte: ?1, $lte: ?2 } } }",
        "{ $count: 'totalMensagens' }"
    })
    Optional<Long> contarMensagensPorNomeEPeriodo(String nome, Date inicio, Date fim);
}
