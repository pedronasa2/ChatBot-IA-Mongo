package br.com.painel.superusuario.model.vo;

import jakarta.validation.constraints.*;

import java.util.*;

public record DadosPromptGlobal(
        @NotBlank @Size(max = 40000) String promptSistema,
        @AssertTrue boolean complementarConfiguracoesEmpresa,
        @Min(0) long revisao) {}
