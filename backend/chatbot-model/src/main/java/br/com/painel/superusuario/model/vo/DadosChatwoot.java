package br.com.painel.superusuario.model.vo;

import jakarta.validation.constraints.*;

public record DadosChatwoot(
        @NotNull @Min(1) @Max(9007199254740991L) Long chatwootAccountId,
        @NotNull @Min(1) @Max(9007199254740991L) Long chatwootInboxId,
        @Min(0) long revisao) {}
