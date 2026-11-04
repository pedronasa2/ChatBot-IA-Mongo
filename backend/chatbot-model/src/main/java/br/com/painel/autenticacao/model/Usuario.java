package br.com.painel.autenticacao.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "usuarios")
public class Usuario {

    @Id public UUID id;

    public UUID empresaId;

    public String nome;

    public String email;

    public String hashSenha;

    public PerfilUsuario perfil = PerfilUsuario.USUARIO;

    public boolean ativo;

    public Convite convite;

    public Instant criadoEm;

    public Instant atualizadoEm;
}
