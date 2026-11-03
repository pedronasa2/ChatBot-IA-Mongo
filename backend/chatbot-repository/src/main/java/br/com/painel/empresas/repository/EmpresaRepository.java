package br.com.painel.empresas.repository;

import br.com.painel.empresas.model.Empresa;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.*;

public interface EmpresaRepository extends MongoRepository<Empresa, UUID> {
    List<Empresa> findByIdIn(Collection<UUID> ids, Sort sort);
}
