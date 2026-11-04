package br.com.painel.autenticacao.repository;

import br.com.painel.autenticacao.model.Usuario;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.*;

public interface UsuarioRepository extends MongoRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);
}
