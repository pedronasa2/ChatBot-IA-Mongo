package br.com.painel.autenticacao.resource;

import br.com.painel.core.service.security.AcessoSuperusuario;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoResource {

    private final AcessoSuperusuario acessoSuperusuario;

    public AutenticacaoResource(AcessoSuperusuario acessoSuperusuario) {
        this.acessoSuperusuario = acessoSuperusuario;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Principal p) {
        return Map.of(
                "email",
                p.getName(),
                "perfil",
                acessoSuperusuario.permitido(p.getName()) ? "SUPERUSUARIO" : "USUARIO");
    }
}
