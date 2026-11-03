package br.com.painel.convites.model.vo;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public record ConviteCriado(
        UUID empresaId, String email, String token, Instant expiraEm, boolean envioSimulado) {}
