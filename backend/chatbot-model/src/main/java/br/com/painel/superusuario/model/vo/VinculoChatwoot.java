package br.com.painel.superusuario.model.vo;

import java.util.UUID;

public record VinculoChatwoot(UUID empresaId, Long accountId, Long inboxId) {}
