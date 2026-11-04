package br.com.painel.superusuario.resource;

import br.com.painel.core.service.security.AcessoSuperusuario;
import br.com.painel.superusuario.model.ConfiguracaoSistema;
import br.com.painel.superusuario.model.vo.*;
import br.com.painel.superusuario.service.*;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

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
    public ConfiguracaoSistema consultarPrompt(Principal usuario) {
        acesso.exigir(usuario);
        return prompts.consultar();
    }

    @PutMapping("/prompt-global")
    public ConfiguracaoSistema salvarPrompt(
            @Valid @RequestBody DadosPromptGlobal dados, Principal usuario) {
        acesso.exigir(usuario);
        return prompts.salvar(dados);
    }

    @GetMapping("/prompt-global/previa/{empresaId}")
    public PreviaPrompt visualizarPrompt(@PathVariable UUID empresaId, Principal usuario) {
        acesso.exigir(usuario);
        return prompts.visualizar(empresaId);
    }

    @GetMapping("/metricas")
    public ResumoMetricas consultarMetricas(
            Principal usuario,
            @RequestParam(required = false) Instant de,
            @RequestParam(required = false) Instant ate,
            @RequestParam(defaultValue = "minute") String intervalo,
            @RequestParam(required = false) UUID empresaId) {
        acesso.exigir(usuario);
        return metricas.consultar(de, ate, intervalo, empresaId);
    }
}
