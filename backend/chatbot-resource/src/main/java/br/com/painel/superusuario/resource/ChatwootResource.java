package br.com.painel.superusuario.resource;

import br.com.painel.empresas.model.vo.EmpresaResposta;
import br.com.painel.empresas.service.EmpresaService;
import br.com.painel.superusuario.model.vo.DadosChatwoot;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/superusuario/empresas/{empresaId}/chatwoot")
public class ChatwootResource {

    private final EmpresaService empresas;

    public ChatwootResource(EmpresaService empresas) {
        this.empresas = empresas;
    }

    @PutMapping
    public EmpresaResposta salvar(
            @PathVariable UUID empresaId,
            @Valid @RequestBody DadosChatwoot dados,
            Principal usuario) {
        return empresas.salvarChatwoot(empresaId, dados, usuario.getName());
    }
}
