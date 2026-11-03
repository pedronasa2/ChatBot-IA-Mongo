package br.com.painel.empresas.resource;

import br.com.painel.empresas.model.vo.DadosEmpresa;
import br.com.painel.empresas.model.vo.EmpresaAcessivel;
import br.com.painel.empresas.model.vo.EmpresaResposta;
import br.com.painel.empresas.service.EmpresaConsultaService;
import br.com.painel.empresas.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaResource {
    private final EmpresaService empresaService;
    private final EmpresaConsultaService empresaConsultaService;

    public EmpresaResource(EmpresaService empresaService, EmpresaConsultaService empresaConsultaService) {
        this.empresaService = empresaService;
        this.empresaConsultaService = empresaConsultaService;
    }

    @GetMapping
    public List<EmpresaAcessivel> listar(Principal usuario) {
        return empresaConsultaService.listar(usuario.getName());
    }

    @GetMapping("/{id}")
    public EmpresaResposta consultar(@PathVariable("id") UUID empresaId, Principal usuario) {
        return empresaService.empresa(empresaId, usuario.getName());
    }

    @PutMapping("/{id}")
    public EmpresaResposta salvar(@PathVariable("id") UUID empresaId, @Valid @RequestBody DadosEmpresa dados, Principal usuario) {
        return empresaService.salvarEmpresa(empresaId, dados, usuario.getName());
    }
}
