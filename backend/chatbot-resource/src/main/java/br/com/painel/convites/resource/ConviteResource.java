package br.com.painel.convites.resource;

import br.com.painel.convites.model.vo.AtivacaoConvite;
import br.com.painel.convites.model.vo.ConviteCriado;
import br.com.painel.convites.model.vo.NovaEmpresa;
import br.com.painel.convites.model.vo.SituacaoConvite;
import br.com.painel.convites.service.ConviteService;
import br.com.painel.core.service.security.AcessoSuperusuario;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
public class ConviteResource {
    private final ConviteService service;
    private final AcessoSuperusuario acesso;

    public ConviteResource(ConviteService service, AcessoSuperusuario acesso) {
        this.service = service;
        this.acesso = acesso;
    }

    @PostMapping("/api/superusuario/empresas")
    @ResponseStatus(HttpStatus.CREATED)
    public ConviteCriado criar(@Valid @RequestBody NovaEmpresa dados, Principal p) {
        return service.criar(dados, acesso.exigir(p));
    }

    @GetMapping("/api/superusuario/empresas/{id}/convite")
    public SituacaoConvite situacao(@PathVariable UUID id, Principal p) {
        acesso.exigir(p);
        return service.situacao(id);
    }

    @PostMapping("/api/superusuario/empresas/{id}/convite")
    public ConviteCriado reenviar(@PathVariable UUID id, Principal p) {
        return service.reenviar(id, acesso.exigir(p));
    }

    @PostMapping("/api/auth/ativar-convite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativar(@Valid @RequestBody AtivacaoConvite dados) {
        service.ativar(dados);
    }
}
