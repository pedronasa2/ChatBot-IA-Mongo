package br.com.painel.superusuario;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoMetricasAtendimento {
  private final JdbcTemplate jdbc;

  public ServicoMetricasAtendimento(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record VolumePorMinuto(Instant minuto, long mensagens) {}
  public record MetricasEmpresa(UUID empresaId, String nomeEmpresa, long totalMensagens, long mensagensUltimas24Horas) {}
  public record ResumoMetricas(
      long mensagensUltimoMinuto, long totalMensagens, long mensagensUltimas24Horas,
      long mensagensRecebidas, long mensagensEnviadas, long errosUltimas24Horas,
      long totalEmpresas, long totalUsuariosAtivos, Instant ultimaMensagem, Instant atualizadoEm,
      List<VolumePorMinuto> volumePorMinuto, List<MetricasEmpresa> porEmpresa) {}

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public ResumoMetricas consultar() {
    Instant agora = Instant.now();
    Timestamp fim = Timestamp.from(agora);
    Timestamp ultimoMinuto = Timestamp.from(agora.minus(1, ChronoUnit.MINUTES));
    Timestamp ultimas24Horas = Timestamp.from(agora.minus(1, ChronoUnit.DAYS));
    var totais = jdbc.queryForMap(
        "select count(*) filter(where resultado='sucesso') as total, count(*) filter(where resultado='sucesso' and ocorrido_em>=?) as minuto,"
            + "count(*) filter(where resultado='sucesso' and ocorrido_em>=?) as dia,"
            + "count(*) filter(where direcao='recebida' and resultado='sucesso') as recebidas,"
            + "count(*) filter(where direcao='enviada' and resultado='sucesso') as enviadas,"
            + "count(*) filter(where resultado='erro' and ocorrido_em>=?) as erros,"
            + "max(ocorrido_em) filter(where resultado='sucesso') as ultima from eventos_mensagens_atendimento where ocorrido_em<=?",
        ultimoMinuto, ultimas24Horas, ultimas24Horas, fim);
    var volume = jdbc.query(
        "select minutos.minuto,count(eventos.id) as mensagens from "
            + "generate_series(date_trunc('minute',?::timestamptz)-interval '14 minutes',"
            + "date_trunc('minute',?::timestamptz),interval '1 minute') as minutos(minuto) "
            + "left join eventos_mensagens_atendimento eventos on eventos.ocorrido_em>=minutos.minuto "
            + "and eventos.ocorrido_em<minutos.minuto+interval '1 minute' and eventos.resultado='sucesso' and eventos.ocorrido_em<=? "
            + "group by minutos.minuto order by minutos.minuto",
        (registro, indice) -> new VolumePorMinuto(registro.getTimestamp("minuto").toInstant(), registro.getLong("mensagens")),
        fim, fim, fim);
    var porEmpresa = jdbc.query(
        "select e.id,e.nome,count(m.id) as total,count(m.id) filter(where m.ocorrido_em>=?) as dia "
            + "from empresas e left join eventos_mensagens_atendimento m on m.empresa_id=e.id and m.resultado='sucesso' and m.ocorrido_em<=? "
            + "group by e.id,e.nome order by total desc,e.nome",
        (registro, indice) -> new MetricasEmpresa(registro.getObject("id", UUID.class), registro.getString("nome"),
            registro.getLong("total"), registro.getLong("dia")), ultimas24Horas, fim);
    Timestamp ultima = (Timestamp) totais.get("ultima");
    return new ResumoMetricas(
        numero(totais, "minuto"), numero(totais, "total"), numero(totais, "dia"),
        numero(totais, "recebidas"), numero(totais, "enviadas"), numero(totais, "erros"),
        jdbc.queryForObject("select count(*) from empresas", Long.class),
        jdbc.queryForObject("select count(*) from usuarios where ativo", Long.class),
        ultima == null ? null : ultima.toInstant(), agora, volume, porEmpresa);
  }

  private long numero(Map<String, Object> totais, String chave) {
    return ((Number) totais.get(chave)).longValue();
  }
}
