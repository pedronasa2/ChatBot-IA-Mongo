package br.com.painel.convites.model.vo;

import jakarta.validation.constraints.*;

public record NovaEmpresa(
        @NotBlank @Size(max = 160) String nome,
        @NotBlank @Size(max = 160) String responsavel,
        @NotBlank @Email @Size(max = 254) String email,
        boolean moduloCandidatosHabilitado,
        boolean moduloAgendaHabilitado) {}
