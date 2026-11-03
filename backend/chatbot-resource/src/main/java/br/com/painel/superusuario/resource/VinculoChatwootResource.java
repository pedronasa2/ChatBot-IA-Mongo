package br.com.painel.superusuario.resource;

import br.com.painel.superusuario.model.vo.DadosVinculoChatwoot;
import br.com.painel.superusuario.model.vo.VinculoChatwoot;
import br.com.painel.superusuario.service.VinculoChatwootService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/superusuario/empresas/{empresaId}/chatwoot")
public class VinculoChatwootResource {
    private final VinculoChatwootService servico;

    public VinculoChatwootResource(VinculoChatwootService servico) {
        this.servico = servico;
    }

    @GetMapping
    public VinculoChatwoot consultar(@PathVariable UUID empresaId, Principal p) {
        return servico.consultar(empresaId, p.getName());
    }

    @PutMapping
    public VinculoChatwoot salvar(
            @PathVariable UUID empresaId,
            @Valid @RequestBody DadosVinculoChatwoot dados,
            Principal p) {
        return servico.salvar(empresaId, dados, p.getName());
    }
}
