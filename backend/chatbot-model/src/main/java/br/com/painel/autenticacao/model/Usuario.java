package br.com.painel.autenticacao.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.*;

import java.util.UUID;

@org.springframework.data.annotation.TypeAlias("br.com.painel.autenticacao.Usuario")
@Document(collection = "usuarios")
public class Usuario {
    @Id
    public UUID id;

    public String email;

    public String nome;

    @Field("criado_em")
    public java.time.Instant criadoEm;

    @Field("atualizado_em")
    public java.time.Instant atualizadoEm;

    @Field("hash_senha")
    public String hashSenha;

    public boolean ativo;

    public boolean superusuario;
}
