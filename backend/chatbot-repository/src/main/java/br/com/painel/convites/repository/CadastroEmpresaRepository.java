package br.com.painel.convites.repository;

import br.com.painel.autenticacao.model.*;
import br.com.painel.convites.model.vo.NovaEmpresa;
import br.com.painel.empresas.model.Empresa;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class CadastroEmpresaRepository {

    private final MongoTemplate mongo;

    public CadastroEmpresaRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void criar(
            NovaEmpresa dados, String email, String senha, UUID empresaId, UUID usuarioId) {
        var agora = Instant.now();
        var empresa = new Empresa();
        empresa.id = empresaId;
        empresa.nome = dados.nome().strip();
        empresa.criadoEm = agora;
        empresa.atualizadoEm = agora;
        mongo.insert(empresa);
        var usuario = new Usuario();
        usuario.id = usuarioId;
        usuario.empresaId = empresaId;
        usuario.nome = dados.responsavel().strip();
        usuario.email = email;
        usuario.hashSenha = senha;
        usuario.perfil = PerfilUsuario.USUARIO;
        usuario.criadoEm = agora;
        usuario.atualizadoEm = agora;
        mongo.insert(usuario);
    }
}
