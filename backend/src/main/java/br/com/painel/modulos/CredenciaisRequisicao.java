package br.com.painel.modulos;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class CredenciaisRequisicao {
  private CredenciaisRequisicao() {}

  static AcessoModulos.Credenciais obter(HttpServletRequest requisicao, Principal usuario) {
    if (requisicao
        .getRequestURI()
        .substring(requisicao.getContextPath().length())
        .startsWith("/api/integracoes/")) {
      return AcessoModulos.Credenciais.integracao(requisicao.getHeader("X-Token-Modulos"));
    }
    if (usuario == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre no painel para continuar.");
    return AcessoModulos.Credenciais.usuario(usuario);
  }
}
