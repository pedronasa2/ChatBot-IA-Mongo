package br.com.painel.superusuario.service;

import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.superusuario.model.vo.DadosPromptGlobal;
import br.com.painel.superusuario.model.vo.PreviaPrompt;
import br.com.painel.superusuario.model.vo.PromptGlobal;
import br.com.painel.superusuario.model.vo.VersaoPromptGlobal;
import br.com.painel.superusuario.repository.ContextoPromptRepository;
import br.com.painel.superusuario.repository.PromptGlobalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PromptGlobalService {
    private final PromptGlobalRepository prompts;
    private final EmpresaRepository empresas;
    private final ContextoPromptRepository configuracoes;

    public PromptGlobalService(
            PromptGlobalRepository prompts,
            EmpresaRepository empresas,
            ContextoPromptRepository configuracoes) {
        this.prompts = prompts;
        this.empresas = empresas;
        this.configuracoes = configuracoes;
    }

    public PromptGlobal consultar() {
        return prompts.consultar();
    }

    @Transactional
    public PromptGlobal salvar(DadosPromptGlobal dados, UUID usuarioId) {
        return prompts.salvar(
                new DadosPromptGlobal(
                        dados.promptSistema().strip(),
                        dados.complementarConfiguracoesEmpresa(),
                        dados.revisao()),
                usuarioId);
    }

    public List<VersaoPromptGlobal> historico() {
        return prompts.historico();
    }

    @Transactional(readOnly = true)
    public PreviaPrompt visualizarRascunho(UUID empresaId) {
        var empresa = empresas.findById(empresaId).orElse(null);
        var bot = configuracoes.rascunho(empresaId);
        if (empresa == null || bot == null) {
            throw new NegocioException(
                    ErroNegocio.NAO_ENCONTRADO, "Empresa ou rascunho não encontrado.");
        }
        return CompositorPrompt.compor(consultar(), empresa, bot);
    }
}
