package br.com.painel.empresas.service;

import br.com.painel.autenticacao.model.PerfilUsuario;
import br.com.painel.autenticacao.repository.UsuarioRepository;
import br.com.painel.empresas.model.vo.EmpresaAcessivel;
import br.com.painel.empresas.repository.EmpresaRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpresaConsultaService {

    private final UsuarioRepository usuarios;

    private final EmpresaRepository empresas;

    public EmpresaConsultaService(UsuarioRepository usuarios, EmpresaRepository empresas) {
        this.usuarios = usuarios;
        this.empresas = empresas;
    }

    public List<EmpresaAcessivel> listar(String email) {
        var u = usuarios.findByEmail(email).filter(usuario -> usuario.ativo).orElse(null);
        if (u == null) {
            return List.of();
        }
        var acessiveis =
                u.perfil == PerfilUsuario.SUPERUSUARIO
                        ? empresas.findAll(Sort.by("nome", "id"))
                        : u.empresaId == null
                                ? List.<br.com.painel.empresas.model.Empresa>of()
                                : empresas.findById(u.empresaId).stream().toList();
        return acessiveis.stream()
                .map(e -> new EmpresaAcessivel(e.id, e.nome, u.perfil.name()))
                .toList();
    }
}
