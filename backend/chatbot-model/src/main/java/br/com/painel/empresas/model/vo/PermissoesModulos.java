package br.com.painel.empresas.model.vo;

import jakarta.validation.constraints.*;

public record PermissoesModulos(
        @NotNull Boolean moduloCandidatosHabilitado,
        @NotNull Boolean moduloAgendaHabilitado,
        @Min(0) long revisao) {}
