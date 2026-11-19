package br.com.painel.superusuario.resource;

import br.com.painel.core.service.security.AcessoSuperusuario;
import br.com.painel.superusuario.model.ConfiguracaoSistema;
import br.com.painel.superusuario.model.vo.*;
import br.com.painel.superusuario.service.*;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/superusuario")
public class SuperusuarioResource {

    private final AcessoSuperusuario acesso;

    private final PromptGlobalService prompts;

    private final MetricasAtendimentoService metricas;
    private final UsoSistemaService uso;

    public SuperusuarioResource(
            AcessoSuperusuario acesso,
            PromptGlobalService prompts,
            MetricasAtendimentoService metricas,
            UsoSistemaService uso) {
        this.acesso = acesso;
        this.prompts = prompts;
        this.metricas = metricas;
        this.uso = uso;
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

    @GetMapping("/metricas/funcionarios-empresa")
    public Map<String, Long> contarFuncionariosPorNome(
            Principal usuario, @RequestParam String nomeEmpresa) {
        acesso.exigir(usuario);
        return Map.of("totalFuncionarios", metricas.contarFuncionariosPorNome(nomeEmpresa));
    }

    @GetMapping("/metricas/usuarios-unicos-hoje")
    public UsuariosUnicosHoje usuariosUnicosHoje(Principal usuario) {
        acesso.exigir(usuario);
        var dia = uso.hoje();
        var estimativa = uso.estimarUsuarios(dia).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        return new UsuariosUnicosHoje(dia, estimativa);
    }

    @GetMapping("/metricas/mensagens-semana")
    public Map<String, Long> contarMensagensNaSemana(
            Principal usuario, @RequestParam String nomeEmpresa) {
        acesso.exigir(usuario);
        return Map.of("totalMensagens", metricas.contarMensagensNaSemana(nomeEmpresa));
    }

    @GetMapping("/metricas/situacao-equipes")
    public List<SituacaoEquipeEmpresa> situacaoEquipes(Principal usuario) {
        acesso.exigir(usuario);
        return metricas.situacaoEquipes();
    }
}
