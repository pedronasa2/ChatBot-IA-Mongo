package br.com.painel.configuracoes;

import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RepositorioConfiguracaoBot extends MongoRepository<ConfiguracaoBot, UUID> {
  Optional<ConfiguracaoBot> findByEmpresaIdAndEstado(UUID empresaId, String estado);
}
