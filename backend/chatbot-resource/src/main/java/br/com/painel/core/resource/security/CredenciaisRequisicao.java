package br.com.painel.core.resource.security;

import br.com.painel.core.model.security.Credenciais;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

public final class CredenciaisRequisicao {
    private CredenciaisRequisicao() {}

    public static Credenciais obter(HttpServletRequest requisicao, Principal usuario) {
        if (requisicao
                .getRequestURI()
                .substring(requisicao.getContextPath().length())
                .startsWith("/api/integracoes/")) {
            return Credenciais.integracao(requisicao.getHeader("X-Token-Modulos"));
        }
        if (usuario == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Entre no painel para continuar.");
        }
        return Credenciais.usuario(usuario);
    }
}
