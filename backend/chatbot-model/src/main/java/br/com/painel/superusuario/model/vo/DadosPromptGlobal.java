package br.com.painel.superusuario.model.vo;

import jakarta.validation.constraints.*;

public record DadosPromptGlobal(
        @NotBlank @Size(max = 40000) String promptGlobal,
        boolean complementarConfiguracoesEmpresa,
        @Min(0) long revisao) {}
