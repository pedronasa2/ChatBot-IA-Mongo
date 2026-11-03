package br.com.painel.autenticacao.service;

import br.com.painel.auditoria.repository.AuditoriaRepository;
import br.com.painel.autenticacao.repository.ProvisionamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProvisionamentoService {
    private final ProvisionamentoRepository repositorio;
    private final AuditoriaRepository auditoria;

    public ProvisionamentoService(
            ProvisionamentoRepository repositorio, AuditoriaRepository auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    @Transactional
    public void criar(String email, String nome, String hash) {
        UUID u = UUID.randomUUID(), c = UUID.randomUUID();
        repositorio.criar(email, nome, hash, u, c);
        auditoria.auditar(c, u, null, "provisionamento_inicial");
    }
}
