package br.com.painel.configuracoes;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioConfiguracaoBot extends JpaRepository<ConfiguracaoBot, UUID> {
  Optional<ConfiguracaoBot> findByEmpresaIdAndEstado(UUID empresaId, String estado);
}
