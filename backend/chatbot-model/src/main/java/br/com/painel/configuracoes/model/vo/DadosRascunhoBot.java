package br.com.painel.configuracoes.model.vo;

import jakarta.validation.constraints.*;

import java.time.*;
import java.util.*;

public record DadosRascunhoBot(
        @NotBlank @Size(max = 80) String nomeBot,
        @Pattern(regexp = "cordial|formal|descontraido") @NotNull String tomVoz,
        boolean usarEmojis,
        @NotNull @Size(max = 10000) String instrucoes,
        @NotNull @Size(max = 5000) String instrucoesTransferencia,
        @Min(0) long revisao) {}
