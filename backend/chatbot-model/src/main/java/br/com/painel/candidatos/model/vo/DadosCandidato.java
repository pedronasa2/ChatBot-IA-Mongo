package br.com.painel.candidatos.model.vo;

import jakarta.validation.constraints.*;

import java.util.*;

public record DadosCandidato(
        @NotBlank @Size(max = 160) String nomeCompleto,
        @NotNull @Email @Size(max = 254) String email,
        @NotNull @Size(max = 40) String telefone,
        @NotNull @Size(max = 160) String cargoPretendido,
        @NotNull @Size(max = 2000) String urlCurriculo,
        @NotNull @Size(max = 10000) String resumoProfissional,
        @NotNull @Size(max = 5000) String observacoes,
        @NotNull @Pattern(regexp = "novo|em_analise|aprovado|reprovado") String situacao,
        boolean arquivado,
        @Min(0) long revisao) {}
