package br.com.painel.superusuario.model.vo;

import java.time.Instant;
import java.util.*;

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
