package br.com.painel.configuracoes.resource;

import br.com.painel.configuracoes.model.vo.DadosConfiguracaoBot;
import br.com.painel.configuracoes.service.ConfiguracaoBotService;
import br.com.painel.empresas.model.vo.EmpresaResposta;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/empresas/{id}/bot")
public class ConfiguracaoBotResource {

    private final ConfiguracaoBotService servico;

    public ConfiguracaoBotResource(ConfiguracaoBotService servico) {
        this.servico = servico;
    }

    @PutMapping
    public EmpresaResposta salvar(
            @PathVariable("id") UUID empresa,
            @Valid @RequestBody DadosConfiguracaoBot dados,
            Principal usuario) {
        return servico.salvar(empresa, dados, usuario.getName());
    }
}
