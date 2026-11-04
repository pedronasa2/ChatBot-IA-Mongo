package br.com.painel.superusuario.model.vo;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record DadosEventoMensagem(@NotNull UUID empresaId, @NotNull Instant enviadoEm) {}
