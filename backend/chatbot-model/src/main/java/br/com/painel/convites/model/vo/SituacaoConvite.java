package br.com.painel.convites.model.vo;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record SituacaoConvite(String email, String responsavel, String estado, Instant expiraEm) {}
