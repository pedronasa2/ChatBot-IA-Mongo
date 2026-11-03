package br.com.painel.empresas.resource;

import br.com.painel.empresas.model.vo.EmpresaResposta;
import br.com.painel.empresas.model.vo.PermissoesModulos;
import br.com.painel.empresas.service.PermissoesModulosService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/superusuario/empresas/{empresaId}/modulos")
public class PermissoesModulosResource {
    private final PermissoesModulosService servico;

    public PermissoesModulosResource(PermissoesModulosService servico) {
        this.servico = servico;
    }

    @PutMapping
    public EmpresaResposta atualizar(
            @PathVariable UUID empresaId,
            @Valid @RequestBody PermissoesModulos dados,
            Principal usuario) {
        return servico.atualizar(empresaId, dados, usuario.getName());
    }
}
