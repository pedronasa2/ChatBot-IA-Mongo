package br.com.painel.configuracoes.model.vo;

import br.com.painel.configuracoes.model.ConfiguracaoBot;

import java.time.Instant;
import java.util.UUID;

public record RascunhoResposta(
        UUID id,
        UUID empresaId,
        int versao,
        String estado,
        String nomeBot,
        String tomVoz,
        boolean usarEmojis,
        String instrucoes,
        String instrucoesTransferencia,
        UUID autorId,
        Instant publicadoEm,
        Long revisao,
        Instant criadoEm,
        Instant atualizadoEm) {
    public static RascunhoResposta de(ConfiguracaoBot e) {
        return new RascunhoResposta(
                e.id,
                e.empresaId,
                e.versao,
                e.estado,
                e.nomeBot,
                e.tomVoz,
                e.usarEmojis,
                e.instrucoes,
                e.instrucoesTransferencia,
                e.autorId,
                e.publicadoEm,
                e.revisao,
                e.criadoEm,
                e.atualizadoEm);
    }
}
