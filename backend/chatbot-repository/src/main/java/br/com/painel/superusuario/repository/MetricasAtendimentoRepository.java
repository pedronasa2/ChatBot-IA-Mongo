package br.com.painel.superusuario.repository;

import static br.com.painel.core.repository.util.ValoresMongo.*;

import br.com.painel.superusuario.model.vo.MetricasEmpresa;
import br.com.painel.superusuario.model.vo.ResumoMetricas;
import br.com.painel.superusuario.model.vo.VolumePorMinuto;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Snapshot query: events, companies and active users, with no per-company round trips. */
@Repository
public class MetricasAtendimentoRepository {
    private final MongoTemplate mongo;

    public MetricasAtendimentoRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private static Document igual(String campo, String valor) {
        return new Document("$eq", List.of("$" + campo, valor));
    }

    private static Document desde(Instant inicio) {
        return new Document("$gte", List.of("$ocorrido_em", Date.from(inicio)));
    }

    private static Document ambos(Object... expressoes) {
        return new Document("$and", Arrays.asList(expressoes));
    }

    private static Document soma(Object condicao) {
        return new Document("$sum", new Document("$cond", List.of(condicao, 1, 0)));
    }

    public ResumoMetricas consultar(Instant agora) {
        Instant dia = agora.minus(1, ChronoUnit.DAYS),
                ultimoMinuto = agora.minus(1, ChronoUnit.MINUTES);
        Instant primeiroMinuto =
                agora.truncatedTo(ChronoUnit.MINUTES).minus(14, ChronoUnit.MINUTES);
        var sucesso = igual("resultado", "sucesso");
        var totais =
                new Document("_id", null)
                        .append("total", soma(sucesso))
                        .append("minuto", soma(ambos(sucesso, desde(ultimoMinuto))))
                        .append("dia", soma(ambos(sucesso, desde(dia))))
                        .append("recebidas", soma(ambos(sucesso, igual("direcao", "recebida"))))
                        .append("enviadas", soma(ambos(sucesso, igual("direcao", "enviada"))))
                        .append("erros", soma(ambos(igual("resultado", "erro"), desde(dia))))
                        .append(
                                "ultima",
                                new Document(
                                        "$max",
                                        new Document(
                                                "$cond",
                                                Arrays.asList(sucesso, "$ocorrido_em", null))));
        var facetas =
                new Document("totais", List.of(new Document("$group", totais)))
                        .append(
                                "volume",
                                List.of(
                                        new Document(
                                                "$match",
                                                new Document("resultado", "sucesso")
                                                        .append(
                                                                "ocorrido_em",
                                                                new Document(
                                                                        "$gte",
                                                                        Date.from(
                                                                                primeiroMinuto)))),
                                        new Document(
                                                "$group",
                                                new Document(
                                                                "_id",
                                                                new Document(
                                                                        "$dateTrunc",
                                                                        new Document(
                                                                                        "date",
                                                                                        "$ocorrido_em")
                                                                                .append(
                                                                                        "unit",
                                                                                        "minute")))
                                                        .append(
                                                                "mensagens",
                                                                new Document("$sum", 1)))))
                        .append(
                                "empresas",
                                List.of(
                                        new Document(
                                                "$match", new Document("resultado", "sucesso")),
                                        new Document(
                                                "$group",
                                                new Document("_id", "$empresa_id")
                                                        .append("total", new Document("$sum", 1))
                                                        .append("dia", soma(desde(dia))))));
        var agregacao =
                Aggregation.newAggregation(
                        contexto ->
                                new Document(
                                        "$match",
                                        new Document(
                                                "ocorrido_em",
                                                new Document("$lte", Date.from(agora)))),
                        contexto -> new Document("$facet", facetas));
        var resultado =
                mongo.aggregate(agregacao, "eventos_mensagens_atendimento", Document.class)
                        .getUniqueMappedResult();
        var listaTotais = resultado.getList("totais", Document.class);
        var resumo = listaTotais.isEmpty() ? new Document() : listaTotais.getFirst();
        var porMinuto = new HashMap<Instant, Long>();
        resultado
                .getList("volume", Document.class)
                .forEach(v -> porMinuto.put(instante(v, "_id"), numero(v, "mensagens")));
        var porEmpresa = new HashMap<UUID, Document>();
        resultado
                .getList("empresas", Document.class)
                .forEach(e -> porEmpresa.put(e.get("_id", UUID.class), e));
        var volume = new ArrayList<VolumePorMinuto>();
        for (int i = 0; i < 15; i++) {
            var minuto = primeiroMinuto.plus(i, ChronoUnit.MINUTES);
            volume.add(new VolumePorMinuto(minuto, porMinuto.getOrDefault(minuto, 0L)));
        }
        var empresas = mongo.find(new Query(), Document.class, "empresas");
        var metricas =
                empresas.stream()
                        .map(
                                e -> {
                                    var contagens =
                                            porEmpresa.getOrDefault(
                                                    e.get("_id", UUID.class), new Document());
                                    return new MetricasEmpresa(
                                            e.get("_id", UUID.class),
                                            e.getString("nome"),
                                            numero(contagens, "total"),
                                            numero(contagens, "dia"));
                                })
                        .sorted(
                                Comparator.comparingLong(MetricasEmpresa::totalMensagens)
                                        .reversed()
                                        .thenComparing(MetricasEmpresa::nomeEmpresa))
                        .toList();
        return new ResumoMetricas(
                numero(resumo, "minuto"),
                numero(resumo, "total"),
                numero(resumo, "dia"),
                numero(resumo, "recebidas"),
                numero(resumo, "enviadas"),
                numero(resumo, "erros"),
                empresas.size(),
                mongo.count(consultaPor("ativo", true), "usuarios"),
                instante(resumo, "ultima"),
                agora,
                volume,
                metricas);
    }
}
