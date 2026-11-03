package br.com.painel.agenda.model.vo;

import java.time.Instant;
import java.util.UUID;

public record Agendamento(
        UUID id,
        UUID empresaId,
        String nomePessoa,
        String email,
        String telefone,
        String assunto,
        String responsavel,
        Instant inicio,
        Instant fim,
        String situacao,
        String observacoes,
        boolean arquivado,
        long revisao,
        Instant criadoEm,
        Instant atualizadoEm) {}
