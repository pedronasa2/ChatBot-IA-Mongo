package br.com.painel.empresas.repository;

import br.com.painel.empresas.model.VinculoUsuarioEmpresa;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.*;

public interface VinculoUsuarioEmpresaRepository
        extends MongoRepository<VinculoUsuarioEmpresa, UUID> {
    boolean existsByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

    List<VinculoUsuarioEmpresa> findByUsuarioId(UUID usuarioId);
}
