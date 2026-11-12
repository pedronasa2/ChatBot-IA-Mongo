package br.com.painel.superusuario.model.vo;

import java.util.UUID;

public record SituacaoEquipeEmpresa(
        UUID empresaId, String nomeEmpresa, int totalFuncionarios, int ativos, int inativos) {}
