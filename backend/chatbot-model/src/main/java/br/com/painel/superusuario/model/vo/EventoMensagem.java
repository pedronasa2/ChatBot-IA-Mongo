package br.com.painel.superusuario.model.vo;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public record EventoMensagem(
        @NotNull UUID empresaId,
        @NotBlank @Size(max = 80) String origem,
        @NotBlank @Size(max = 160) String idEventoOrigem,
        @NotNull @Pattern(regexp = "recebida|enviada") String direcao,
        @NotNull @Pattern(regexp = "sucesso|erro") String resultado,
        @NotNull Instant ocorridoEm) {}
