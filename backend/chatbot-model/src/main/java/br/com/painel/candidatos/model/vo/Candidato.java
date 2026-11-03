package br.com.painel.candidatos.model.vo;

import java.time.Instant;
import java.util.UUID;

public record Candidato(
        UUID id,
        UUID empresaId,
        String nomeCompleto,
        String email,
        String telefone,
        String cargoPretendido,
        String urlCurriculo,
        String resumoProfissional,
        String observacoes,
        String situacao,
        boolean arquivado,
        long revisao,
        Instant recebidoEm,
        Instant atualizadoEm) {}
