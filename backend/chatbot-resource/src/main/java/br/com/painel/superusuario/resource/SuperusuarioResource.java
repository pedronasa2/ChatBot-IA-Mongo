package br.com.painel.superusuario.resource;

import br.com.painel.core.service.security.AcessoSuperusuario;
import br.com.painel.superusuario.model.vo.DadosPromptGlobal;
import br.com.painel.superusuario.model.vo.PreviaPrompt;
import br.com.painel.superusuario.model.vo.PromptGlobal;
import br.com.painel.superusuario.model.vo.ResumoMetricas;
import br.com.painel.superusuario.model.vo.VersaoPromptGlobal;
import br.com.painel.superusuario.service.MetricasAtendimentoService;
import br.com.painel.superusuario.service.PromptGlobalService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/superusuario")
public class SuperusuarioResource {
    private final AcessoSuperusuario acesso;
    private final PromptGlobalService prompts;
    private final MetricasAtendimentoService metricas;

    public SuperusuarioResource(
            AcessoSuperusuario acesso,
            PromptGlobalService prompts,
            MetricasAtendimentoService metricas) {
        this.acesso = acesso;
        this.prompts = prompts;
        this.metricas = metricas;
    }

    @GetMapping("/prompt-global")
    public PromptGlobal consultarPrompt(Principal usuario) {
        acesso.exigir(usuario);
        return prompts.consultar();
    }

    @PutMapping("/prompt-global")
    public PromptGlobal salvarPrompt(
            @Valid @RequestBody DadosPromptGlobal dados, Principal usuario) {
        return prompts.salvar(dados, acesso.exigir(usuario));
    }

    @GetMapping("/prompt-global/historico")
    public List<VersaoPromptGlobal> historicoPrompt(Principal usuario) {
        acesso.exigir(usuario);
        return prompts.historico();
    }

    @GetMapping("/prompt-global/previa/{empresaId}")
    public PreviaPrompt visualizarPrompt(@PathVariable UUID empresaId, Principal usuario) {
        acesso.exigir(usuario);
        return prompts.visualizarRascunho(empresaId);
    }

    @GetMapping("/metricas")
    public ResumoMetricas consultarMetricas(Principal usuario) {
        acesso.exigir(usuario);
        return metricas.consultar();
    }
}
