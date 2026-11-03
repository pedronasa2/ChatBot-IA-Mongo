package br.com.painel.superusuario.model.vo;

import jakarta.validation.constraints.*;

public record DadosVinculoChatwoot(
        @NotNull @Min(1) @Max(9007199254740991L) Long accountId,
        @NotNull @Min(1) @Max(9007199254740991L) Long inboxId) {}
