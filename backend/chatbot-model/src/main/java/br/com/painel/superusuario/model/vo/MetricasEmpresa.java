package br.com.painel.superusuario.model.vo;

import java.util.*;

public record MetricasEmpresa(
        UUID empresaId, String nomeEmpresa, long totalMensagens, long mensagensUltimas24Horas) {}
