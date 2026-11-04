package br.com.painel.core.service.security;

import br.com.painel.autenticacao.model.PerfilUsuario;
import br.com.painel.autenticacao.repository.UsuarioRepository;
import br.com.painel.core.model.exception.*;
import br.com.painel.empresas.repository.EmpresaRepository;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AcessoEmpresa {

    private final UsuarioRepository usuarios;

    private final EmpresaRepository empresas;

    public AcessoEmpresa(UsuarioRepository usuarios, EmpresaRepository empresas) {
        this.usuarios = usuarios;
        this.empresas = empresas;
    }

    public UUID exigirVinculo(UUID empresa, String email) {
        var usuario = usuarios.findByEmail(email).filter(u -> u.ativo).orElse(null);
        if (usuario == null
                || !empresas.existsById(empresa)
                || (usuario.perfil != PerfilUsuario.SUPERUSUARIO
                        && !empresa.equals(usuario.empresaId))) {
            throw new NegocioException(ErroNegocio.NAO_ENCONTRADO, "Empresa não encontrada");
        }
        return usuario.id;
    }
}
