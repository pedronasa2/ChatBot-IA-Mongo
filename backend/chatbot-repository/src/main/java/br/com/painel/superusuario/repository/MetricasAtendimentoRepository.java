package br.com.painel.superusuario.repository;

import static br.com.painel.core.repository.util.ValoresMongo.*;

import br.com.painel.superusuario.model.vo.*;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.*;

@Repository
public class MetricasAtendimentoRepository {

    private final MongoTemplate mongo;

    public MetricasAtendimentoRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private Document desde(Instant inicio) {
        return new Document(
                "$sum",
                new Document(
                        "$cond",
                        List.of(
                                new Document("$gte", List.of("$enviadoEm", Date.from(inicio))),
                                1,
                                0)));
    }

    public ResumoMetricas consultar(
            Instant agora, Instant de, Instant ate, String intervalo, UUID empresaId, String fuso) {
        var filtro = new Document("enviadoEm", new Document("$lte", Date.from(agora)));
        if (empresaId != null) {
            filtro.append("empresaId", empresaId);
        }
        var grupo =
                new Document("_id", null)
                        .append("total", new Document("$sum", 1))
                        .append("minuto", desde(agora.minusSeconds(60)))
                        .append("dia", desde(agora.minusSeconds(86400)))
                        .append("ultima", new Document("$max", "$enviadoEm"));
        var porEmpresa =
                new Document("_id", "$empresaId")
                        .append("total", new Document("$sum", 1))
                        .append("dia", desde(agora.minusSeconds(86400)));
        var periodo =
                new Document(
                        "enviadoEm",
                        new Document("$gte", Date.from(de)).append("$lt", Date.from(ate)));
        var truncar =
                new Document(
                        "$dateTrunc",
                        new Document("date", "$enviadoEm")
                                .append("unit", intervalo)
                                .append("timezone", fuso));
        var facetas =
                new Document("totais", List.of(new Document("$group", grupo)))
                        .append("empresas", List.of(new Document("$group", porEmpresa)))
                        .append(
                                "volume",
                                List.of(
                                        new Document("$match", periodo),
                                        new Document(
                                                "$group",
                                                new Document("_id", truncar)
                                                        .append(
                                                                "mensagens",
                                                                new Document("$sum", 1))),
                                        new Document("$sort", new Document("_id", 1))));
        var resultado =
                mongo.aggregate(
                                Aggregation.newAggregation(
                                        c -> new Document("$match", filtro),
                                        c -> new Document("$facet", facetas)),
                                "eventos_mensagens",
                                Document.class)
                        .getUniqueMappedResult();
        var totais = resultado.getList("totais", Document.class);
        var resumo = totais.isEmpty() ? new Document() : totais.getFirst();
        var contagens = new HashMap<UUID, Document>();
        resultado
                .getList("empresas", Document.class)
                .forEach(e -> contagens.put(e.get("_id", UUID.class), e));
        var empresas =
                mongo.find(
                        empresaId == null ? new Query() : consultaPor("_id", empresaId),
                        Document.class,
                        "empresas");
        var metricas =
                empresas.stream()
                        .map(
                                e -> {
                                    var valores =
                                            contagens.getOrDefault(
                                                    e.get("_id", UUID.class), new Document());
                                    return new MetricasEmpresa(
                                            e.get("_id", UUID.class),
                                            e.getString("nome"),
                                            numero(valores, "total"),
                                            numero(valores, "dia"));
                                })
                        .sorted(Comparator.comparing(MetricasEmpresa::nomeEmpresa))
                        .toList();
        var volumes = new HashMap<Instant, Long>();
        resultado
                .getList("volume", Document.class)
                .forEach(v -> volumes.put(instante(v, "_id"), numero(v, "mensagens")));
        var inicio = de.atZone(ZoneId.of(fuso));
        inicio =
                switch (intervalo) {
                    case "minute" -> inicio.withSecond(0).withNano(0);
                    case "hour" -> inicio.withMinute(0).withSecond(0).withNano(0);
                    default -> inicio.toLocalDate().atStartOfDay(ZoneId.of(fuso));
                };
        var volume = new ArrayList<PeriodoMensagens>();
        for (var atual = inicio;
                atual.toInstant().isBefore(ate);
                atual =
                        switch (intervalo) {
                            case "minute" -> atual.plusMinutes(1);
                            case "hour" -> atual.plusHours(1);
                            default -> atual.plusDays(1);
                        }) {
            volume.add(
                    new PeriodoMensagens(
                            atual.toInstant(), volumes.getOrDefault(atual.toInstant(), 0L)));
        }
        return new ResumoMetricas(
                numero(resumo, "minuto"),
                numero(resumo, "total"),
                numero(resumo, "dia"),
                empresas.size(),
                mongo.count(consultaPor("ativo", true), "usuarios"),
                instante(resumo, "ultima"),
                agora,
                fuso,
                volume,
                metricas);
    }
}
