package br.com.painel.agenda.model.vo;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.*;

public record DadosAgendamento(
        @NotBlank @Size(max = 160) String nomePessoa,
        @NotNull @Email @Size(max = 254) String email,
        @NotNull @Size(max = 40) String telefone,
        @NotBlank @Size(max = 200) String assunto,
        @NotNull @Size(max = 160) String responsavel,
        @NotNull Instant inicio,
        @NotNull Instant fim,
        @NotNull @Pattern(regexp = "agendado|confirmado|concluido|cancelado") String situacao,
        @NotNull @Size(max = 5000) String observacoes,
        boolean arquivado,
        @Min(0) long revisao) {}
