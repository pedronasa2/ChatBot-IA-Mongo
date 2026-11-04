package br.com.painel.superusuario.service;

import br.com.painel.core.model.exception.*;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.superusuario.model.ConfiguracaoSistema;
import br.com.painel.superusuario.model.vo.*;
import br.com.painel.superusuario.repository.PromptGlobalRepository;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PromptGlobalService {

    private final PromptGlobalRepository prompts;

    private final EmpresaRepository empresas;

    public PromptGlobalService(PromptGlobalRepository prompts, EmpresaRepository empresas) {
        this.prompts = prompts;
        this.empresas = empresas;
    }

    public ConfiguracaoSistema consultar() {
        return prompts.consultar();
    }

    public ConfiguracaoSistema salvar(DadosPromptGlobal dados) {
        return prompts.salvar(
                new DadosPromptGlobal(
                        dados.promptGlobal().strip(),
                        dados.complementarConfiguracoesEmpresa(),
                        dados.revisao()));
    }

    public PreviaPrompt visualizar(UUID empresaId) {
        var empresa =
                empresas.findById(empresaId)
                        .orElseThrow(
                                () ->
                                        new NegocioException(
                                                ErroNegocio.NAO_ENCONTRADO,
                                                "Empresa não encontrada."));
        return CompositorPrompt.compor(consultar(), empresa, empresa.bot);
    }
}
