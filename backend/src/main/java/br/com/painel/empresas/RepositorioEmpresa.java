package br.com.painel.empresas;

import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RepositorioEmpresa extends MongoRepository<Empresa, UUID> {}
