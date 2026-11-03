package br.com.painel.superusuario.model.vo;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.*;

public record VersaoPromptGlobal(
        String promptSistema,
        boolean complementarConfiguracoesEmpresa,
        long revisao,
        Instant criadoEm,
        String usuarioResponsavelEmail) {}
