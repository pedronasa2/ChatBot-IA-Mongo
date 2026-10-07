package br.com.painel.autenticacao;

import java.security.Principal;
import java.util.Map;
import br.com.painel.superusuario.AcessoSuperusuario;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacao {
  private final AcessoSuperusuario acessoSuperusuario;

  public ControladorAutenticacao(AcessoSuperusuario acessoSuperusuario) {
    this.acessoSuperusuario = acessoSuperusuario;
  }
  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @GetMapping("/me")
  public Map<String, Object> me(Principal p) {
    return Map.of("email", p.getName(), "superusuario", acessoSuperusuario.permitido(p.getName()));
  }
}
