package br.com.painel.empresas;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioEmpresa extends JpaRepository<Empresa, UUID> {}
