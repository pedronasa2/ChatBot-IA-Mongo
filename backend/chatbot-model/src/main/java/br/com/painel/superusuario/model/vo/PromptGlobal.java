package br.com.painel.superusuario.model.vo;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.*;

public record PromptGlobal(
        String promptSistema,
        boolean complementarConfiguracoesEmpresa,
        long revisao,
        Instant atualizadoEm,
        String atualizadoPorEmail) {}
