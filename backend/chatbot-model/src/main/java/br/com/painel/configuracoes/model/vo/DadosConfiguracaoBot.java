package br.com.painel.configuracoes.model.vo;

import jakarta.validation.constraints.*;

public record DadosConfiguracaoBot(
        @NotBlank @Size(max = 80) String nome,
        @NotNull @Pattern(regexp = "cordial|formal|descontraido") String tomVoz,
        boolean usarEmojis,
        @NotNull @Size(max = 10000) String instrucoes,
        @NotNull @Size(max = 5000) String instrucoesTransferencia,
        @Min(0) long revisao) {}
