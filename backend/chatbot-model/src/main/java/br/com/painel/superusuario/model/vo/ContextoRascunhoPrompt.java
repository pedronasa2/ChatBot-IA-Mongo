package br.com.painel.superusuario.model.vo;

import org.springframework.data.mongodb.core.mapping.Field;

/** Persisted projection: missing emoji flags remain null, as in the original preview. */
public record ContextoRascunhoPrompt(
        @Field("nome_bot") String nomeBot,
        @Field("tom_voz") String tomVoz,
        @Field("usar_emojis") Boolean usarEmojis,
        String instrucoes,
        @Field("instrucoes_transferencia") String instrucoesTransferencia) {}
