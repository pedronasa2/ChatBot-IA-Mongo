package br.com.painel.core.service.security;

import br.com.painel.autenticacao.model.Usuario;
import br.com.painel.autenticacao.repository.UsuarioRepository;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;

import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
public class AcessoSuperusuario {
    private final UsuarioRepository usuarios;

    public AcessoSuperusuario(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public boolean permitido(String email) {
        return usuario(email) != null;
    }

    private Usuario usuario(String email) {
        return usuarios.findByEmail(email)
                .filter(usuarioAtual -> usuarioAtual.ativo && usuarioAtual.superusuario)
                .orElse(null);
    }

    public UUID exigir(Principal p) {
        var usuarioAtual = usuario(p.getName());
        if (usuarioAtual == null) {
            throw new NegocioException(
                    ErroNegocio.ACESSO_NEGADO, "Acesso exclusivo de superusuário.");
        }
        return usuarioAtual.id;
    }
}
