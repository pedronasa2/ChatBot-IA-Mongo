package br.com.painel.autenticacao.service;

import br.com.painel.autenticacao.repository.ProvisionamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProvisionamentoService {

    private final ProvisionamentoRepository repositorio;

    public ProvisionamentoService(ProvisionamentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void criar(String email, String nome, String hash) {
        UUID u = UUID.randomUUID(), c = UUID.randomUUID();
        repositorio.criar(email, nome, hash, u, c);
    }
}
