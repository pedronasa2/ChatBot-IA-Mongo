package br.com.painel.core.service.security;

import br.com.painel.autenticacao.repository.UsuarioRepository;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.empresas.repository.VinculoUsuarioEmpresaRepository;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AcessoEmpresa {
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final VinculoUsuarioEmpresaRepository vinculos;

    public AcessoEmpresa(
            UsuarioRepository usuarios,
            EmpresaRepository empresas,
            VinculoUsuarioEmpresaRepository vinculos) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.vinculos = vinculos;
    }

    public UUID exigirVinculo(UUID empresa, String email) {
        var usuario =
                usuarios.findByEmail(email).filter(usuarioAtual -> usuarioAtual.ativo).orElse(null);
        if (usuario == null
                || !empresas.existsById(empresa)
                || (!usuario.superusuario
                        && !vinculos.existsByUsuarioIdAndEmpresaId(usuario.id, empresa))) {
            throw new NegocioException(ErroNegocio.NAO_ENCONTRADO, "Empresa não encontrada");
        }
        return usuario.id;
    }
}
