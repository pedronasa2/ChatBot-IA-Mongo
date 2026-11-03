/** Test-only reference: pre-aggregation query behavior for exact comparison. */
package br.com.painel.superusuario;

import static br.com.painel.core.repository.util.ValoresMongo.*;

import br.com.painel.superusuario.model.vo.MetricasEmpresa;
import br.com.painel.superusuario.model.vo.ResumoMetricas;
import br.com.painel.superusuario.model.vo.VolumePorMinuto;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RepositorioMetricasReferencia {
    private final org.springframework.data.mongodb.core.MongoTemplate mongo;

    public RepositorioMetricasReferencia(
            org.springframework.data.mongodb.core.MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private long contar(Instant ate, Instant de, String direcao, String resultado, UUID empresa) {
        var c = Criteria.where("ocorrido_em").lte(Date.from(ate));
        if (de != null) {
            c.gte(Date.from(de));
        }
        var consultaPor = Query.query(c);
        if (direcao != null) {
            consultaPor.addCriteria(Criteria.where("direcao").is(direcao));
        }
        if (resultado != null) {
            consultaPor.addCriteria(Criteria.where("resultado").is(resultado));
        }
        if (empresa != null) {
            consultaPor.addCriteria(Criteria.where("empresa_id").is(empresa));
        }
        return mongo.count(consultaPor, "eventos_mensagens_atendimento");
    }

    public ResumoMetricas consultar(Instant agora) {
        Instant dia = agora.minus(1, ChronoUnit.DAYS);
        var ultima =
                mongo.findOne(
                        consultaPor("resultado", "sucesso")
                                .addCriteria(Criteria.where("ocorrido_em").lte(Date.from(agora)))
                                .with(Sort.by(Sort.Direction.DESC, "ocorrido_em")),
                        Document.class,
                        "eventos_mensagens_atendimento");
        var volume = new ArrayList<VolumePorMinuto>();
        for (int i = 14; i >= 0; i--) {
            Instant minuto = agora.truncatedTo(ChronoUnit.MINUTES).minus(i, ChronoUnit.MINUTES);
            var consulta =
                    consultaPor("resultado", "sucesso")
                            .addCriteria(
                                    Criteria.where("ocorrido_em")
                                            .gte(Date.from(minuto))
                                            .lt(Date.from(minuto.plus(1, ChronoUnit.MINUTES))));
            consulta.addCriteria(
                    new Criteria()
                            .andOperator(Criteria.where("ocorrido_em").lte(Date.from(agora))));
            volume.add(
                    new VolumePorMinuto(
                            minuto, mongo.count(consulta, "eventos_mensagens_atendimento")));
        }
        var porEmpresa =
                mongo.find(new Query(), Document.class, "empresas").stream()
                        .map(
                                e ->
                                        new MetricasEmpresa(
                                                e.get("_id", UUID.class),
                                                e.getString("nome"),
                                                contar(
                                                        agora,
                                                        null,
                                                        null,
                                                        "sucesso",
                                                        e.get("_id", UUID.class)),
                                                contar(
                                                        agora,
                                                        dia,
                                                        null,
                                                        "sucesso",
                                                        e.get("_id", UUID.class))))
                        .sorted(
                                Comparator.comparingLong(MetricasEmpresa::totalMensagens)
                                        .reversed()
                                        .thenComparing(MetricasEmpresa::nomeEmpresa))
                        .toList();
        return new ResumoMetricas(
                contar(agora, agora.minus(1, ChronoUnit.MINUTES), null, "sucesso", null),
                contar(agora, null, null, "sucesso", null),
                contar(agora, dia, null, "sucesso", null),
                contar(agora, null, "recebida", "sucesso", null),
                contar(agora, null, "enviada", "sucesso", null),
                contar(agora, dia, null, "erro", null),
                mongo.count(new Query(), "empresas"),
                mongo.count(consultaPor("ativo", true), "usuarios"),
                ultima == null ? null : instante(ultima, "ocorrido_em"),
                agora,
                volume,
                porEmpresa);
    }
}
