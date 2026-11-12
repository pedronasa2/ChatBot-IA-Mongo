package br.com.painel.superusuario.service;

import br.com.painel.core.model.exception.*;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.superusuario.model.vo.ResumoMetricas;
import br.com.painel.superusuario.model.vo.SituacaoEquipeEmpresa;
import br.com.painel.superusuario.repository.MetricasAtendimentoRepository;

import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class MetricasAtendimentoService {

    private final MetricasAtendimentoRepository metricas;

    private final EmpresaRepository empresas;

    public MetricasAtendimentoService(
            MetricasAtendimentoRepository metricas, EmpresaRepository empresas) {
        this.metricas = metricas;
        this.empresas = empresas;
    }

    public long contarMensagensNaSemana(String nomeEmpresa) {
        var agora = Instant.now();
        var fuso = ZoneId.of("America/Sao_Paulo");
        var inicio = agora.atZone(fuso).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(fuso).toInstant();
        return empresas.contarMensagensPorNomeEPeriodo(nomeEmpresa, Date.from(inicio), Date.from(agora))
                .orElse(0L);
    }

    public List<SituacaoEquipeEmpresa> situacaoEquipes() {
        return metricas.situacaoEquipes();
    }

    public ResumoMetricas consultar(Instant de, Instant ate, String intervalo, UUID empresaId) {
        if (!Set.of("minute", "hour", "day").contains(intervalo)) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS, "Intervalo inválido. Use minute, hour ou day.");
        }
        var agora = Instant.now();
        var fim = ate == null ? agora : ate;
        var inicio =
                de == null
                        ? fim.minusSeconds(
                                switch (intervalo) {
                                    case "minute" -> 900;
                                    case "hour" -> 86400;
                                    default -> 2592000;
                                })
                        : de;
        var segundos = Duration.between(inicio, fim).getSeconds();
        var passo =
                switch (intervalo) {
                    case "minute" -> 60;
                    case "hour" -> 3600;
                    default -> 86400;
                };
        if (!inicio.isBefore(fim) || fim.isAfter(agora) || segundos / passo > 1000) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS,
                    "Informe um período passado válido com até 1.000 intervalos.");
        }
        var fuso = "UTC";
        if (empresaId != null) {
            var e =
                    empresas.findById(empresaId)
                            .orElseThrow(
                                    () ->
                                            new NegocioException(
                                                    ErroNegocio.NAO_ENCONTRADO,
                                                    "Empresa não encontrada."));
            fuso = e.fusoHorario;
        }
        return metricas.consultar(agora, inicio, fim, intervalo, empresaId, fuso);
    }
}
