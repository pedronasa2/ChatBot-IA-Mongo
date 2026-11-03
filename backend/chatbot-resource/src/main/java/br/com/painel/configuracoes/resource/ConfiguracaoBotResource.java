package br.com.painel.configuracoes.resource;

import br.com.painel.configuracoes.model.vo.DadosRascunhoBot;
import br.com.painel.configuracoes.model.vo.RascunhoResposta;
import br.com.painel.configuracoes.service.ConfiguracaoBotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/empresas/{id}/rascunho")
public class ConfiguracaoBotResource {
    private final ConfiguracaoBotService configuracaoBotService;

    public ConfiguracaoBotResource(ConfiguracaoBotService configuracaoBotService) {
        this.configuracaoBotService = configuracaoBotService;
    }

    @GetMapping
    public RascunhoResposta consultar(@PathVariable("id") UUID empresaId, Principal usuario) {
        return configuracaoBotService.rascunho(empresaId, usuario.getName());
    }

    @PutMapping
    public RascunhoResposta salvar(@PathVariable("id") UUID empresaId, @Valid @RequestBody DadosRascunhoBot dados, Principal usuario) {
        return configuracaoBotService.salvarRascunho(empresaId, dados, usuario.getName());
    }
}
