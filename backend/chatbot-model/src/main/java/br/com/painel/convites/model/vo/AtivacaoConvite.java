package br.com.painel.convites.model.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtivacaoConvite(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 6, max = 21) String senha,
        @NotBlank @Size(min = 6, max = 21) String confirmacaoSenha) {}
