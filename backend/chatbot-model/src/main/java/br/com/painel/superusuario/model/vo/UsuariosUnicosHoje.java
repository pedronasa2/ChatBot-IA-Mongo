package br.com.painel.superusuario.model.vo;

import java.time.LocalDate;

public record UsuariosUnicosHoje(LocalDate data, long usuariosUnicosEstimados) {}
