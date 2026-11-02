package br.com.painel.superusuario;

import static br.com.painel.persistencia.Documentos.*;

import br.com.painel.persistencia.Documentos;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoMetricasAtendimento {
  private final Documentos documentos;

  public ServicoMetricasAtendimento(Documentos documentos) {
    this.documentos = documentos;
  }

  public record VolumePorMinuto(Instant minuto, long mensagens) {}

  public record MetricasEmpresa(
      UUID empresaId, String nomeEmpresa, long totalMensagens, long mensagensUltimas24Horas) {}

  public record ResumoMetricas(
      long mensagensUltimoMinuto,
      long totalMensagens,
      long mensagensUltimas24Horas,
      long mensagensRecebidas,
      long mensagensEnviadas,
      long errosUltimas24Horas,
      long totalEmpresas,
      long totalUsuariosAtivos,
      Instant ultimaMensagem,
      Instant atualizadoEm,
      List<VolumePorMinuto> volumePorMinuto,
      List<MetricasEmpresa> porEmpresa) {}

  private long contar(Instant ate, Instant de, String direcao, String resultado, UUID empresa) {
    var c = Criteria.where("ocorrido_em").lte(Date.from(ate));
    if (de != null) c.gte(Date.from(de));
    var q = Query.query(c);
    if (direcao != null) q.addCriteria(Criteria.where("direcao").is(direcao));
    if (resultado != null) q.addCriteria(Criteria.where("resultado").is(resultado));
    if (empresa != null) q.addCriteria(Criteria.where("empresa_id").is(empresa));
    return documentos.contar("eventos_mensagens_atendimento", q);
  }

  @Transactional(readOnly = true)
  public ResumoMetricas consultar() {
    Instant agora = Instant.now(), dia = agora.minus(1, ChronoUnit.DAYS);
    var ultima =
        documentos.um(
            "eventos_mensagens_atendimento",
            q("resultado", "sucesso")
                .addCriteria(Criteria.where("ocorrido_em").lte(Date.from(agora)))
                .with(Sort.by(Sort.Direction.DESC, "ocorrido_em")));
    var volume = new ArrayList<VolumePorMinuto>();
    for (int i = 14; i >= 0; i--) {
      Instant minuto = agora.truncatedTo(ChronoUnit.MINUTES).minus(i, ChronoUnit.MINUTES);
      var consulta =
          q("resultado", "sucesso")
              .addCriteria(
                  Criteria.where("ocorrido_em")
                      .gte(Date.from(minuto))
                      .lt(Date.from(minuto.plus(1, ChronoUnit.MINUTES))));
      consulta.addCriteria(
          new Criteria().andOperator(Criteria.where("ocorrido_em").lte(Date.from(agora))));
      volume.add(
          new VolumePorMinuto(
              minuto, documentos.contar("eventos_mensagens_atendimento", consulta)));
    }
    var porEmpresa =
        documentos.lista("empresas", new Query()).stream()
            .map(
                e ->
                    new MetricasEmpresa(
                        e.get("_id", UUID.class),
                        e.getString("nome"),
                        contar(agora, null, null, "sucesso", e.get("_id", UUID.class)),
                        contar(agora, dia, null, "sucesso", e.get("_id", UUID.class))))
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
        documentos.contar("empresas", new Query()),
        documentos.contar("usuarios", q("ativo", true)),
        ultima == null ? null : instante(ultima, "ocorrido_em"),
        agora,
        volume,
        porEmpresa);
  }
}
