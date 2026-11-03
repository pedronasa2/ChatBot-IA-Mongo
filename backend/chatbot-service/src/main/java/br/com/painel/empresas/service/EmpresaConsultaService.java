package br.com.painel.empresas.service;

import br.com.painel.autenticacao.repository.UsuarioRepository;
import br.com.painel.empresas.model.vo.EmpresaAcessivel;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.empresas.repository.VinculoUsuarioEmpresaRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EmpresaConsultaService {
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final VinculoUsuarioEmpresaRepository vinculos;

    public EmpresaConsultaService(
            UsuarioRepository usuarios,
            EmpresaRepository empresas,
            VinculoUsuarioEmpresaRepository vinculos) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.vinculos = vinculos;
    }

    public List<EmpresaAcessivel> listar(String email) {
        var usuarioAtual =
                usuarios.findByEmail(email).filter(usuario -> usuario.ativo).orElse(null);
        if (usuarioAtual == null) {
            return List.of();
        }
        var papeis = new HashMap<UUID, String>();
        vinculos.findByUsuarioId(usuarioAtual.id)
                .forEach(vinculo -> papeis.put(vinculo.empresaId, vinculo.papel));
        return (usuarioAtual.superusuario
                        ? empresas.findAll(Sort.by("nome", "id"))
                        : empresas.findByIdIn(papeis.keySet(), Sort.by("nome", "id")))
                .stream()
                        .map(
                                empresa ->
                                        new EmpresaAcessivel(
                                                empresa.id,
                                                empresa.nome,
                                                usuarioAtual.superusuario
                                                        ? "superusuario"
                                                        : papeis.get(empresa.id)))
                        .toList();
    }
}
