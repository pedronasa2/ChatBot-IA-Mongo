package br.com.painel.core.service;

import br.com.painel.core.repository.MigracaoModelagemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MigracaoModelagemService {

    private final MigracaoModelagemRepository repositorio;

    public MigracaoModelagemService(MigracaoModelagemRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void converter() {
        repositorio.converter();
    }

    public void removerLegadas() {
        repositorio.removerLegadas();
    }
}
