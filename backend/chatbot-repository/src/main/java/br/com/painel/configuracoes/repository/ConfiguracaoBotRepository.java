package br.com.painel.configuracoes.repository;

import br.com.painel.configuracoes.model.ConfiguracaoBot;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.*;

public interface ConfiguracaoBotRepository extends MongoRepository<ConfiguracaoBot, UUID> {
    Optional<ConfiguracaoBot> findByEmpresaIdAndEstado(UUID empresaId, String estado);
}
