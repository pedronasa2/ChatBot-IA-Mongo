package br.com.painel.core.model.security;

import java.security.Principal;

public record Credenciais(String emailUsuario, String tokenIntegracao) {
    public static Credenciais usuario(Principal usuario) {
        return new Credenciais(usuario.getName(), null);
    }

    public static Credenciais integracao(String token) {
        return new Credenciais(null, token);
    }
}
