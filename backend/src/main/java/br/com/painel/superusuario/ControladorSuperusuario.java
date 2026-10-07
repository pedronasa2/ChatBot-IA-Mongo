package br.com.painel.superusuario;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/superusuario")
public class ControladorSuperusuario {
  private final AcessoSuperusuario acesso;
  private final ServicoPromptGlobal prompts;
  private final ServicoMetricasAtendimento metricas;

  public ControladorSuperusuario(AcessoSuperusuario acesso, ServicoPromptGlobal prompts, ServicoMetricasAtendimento metricas) {
    this.acesso = acesso;
    this.prompts = prompts;
    this.metricas = metricas;
  }

  @GetMapping("/prompt-global")
  public ServicoPromptGlobal.PromptGlobal consultarPrompt(Principal usuario) {
    acesso.exigir(usuario);
    return prompts.consultar();
  }

  @PutMapping("/prompt-global")
  public ServicoPromptGlobal.PromptGlobal salvarPrompt(@Valid @RequestBody ServicoPromptGlobal.DadosPromptGlobal dados, Principal usuario) {
    return prompts.salvar(dados, acesso.exigir(usuario));
  }

  @GetMapping("/prompt-global/historico")
  public List<ServicoPromptGlobal.VersaoPromptGlobal> historicoPrompt(Principal usuario) {
    acesso.exigir(usuario);
    return prompts.historico();
  }

  @GetMapping("/prompt-global/previa/{empresaId}")
  public ServicoPromptGlobal.PreviaPrompt visualizarPrompt(@PathVariable UUID empresaId, Principal usuario) {
    acesso.exigir(usuario);
    return prompts.visualizarRascunho(empresaId);
  }

  @GetMapping("/metricas")
  public ServicoMetricasAtendimento.ResumoMetricas consultarMetricas(Principal usuario) {
    acesso.exigir(usuario);
    return metricas.consultar();
  }
}
