package br.com.painel.superusuario.model.vo;

import java.time.Instant;
import java.util.List;

public record ResumoMetricas(
        long mensagensUltimoMinuto,
        long totalMensagens,
        long mensagensUltimas24Horas,
        long totalEmpresas,
        long totalUsuariosAtivos,
        Instant ultimaMensagem,
        Instant atualizadoEm,
        String fusoHorario,
        List<PeriodoMensagens> volume,
        List<MetricasEmpresa> porEmpresa) {}
