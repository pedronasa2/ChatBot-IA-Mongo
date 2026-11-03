package br.com.painel.candidatos.resource;

import br.com.painel.candidatos.model.vo.Candidato;
import br.com.painel.candidatos.model.vo.DadosCandidato;
import br.com.painel.candidatos.service.CandidatoService;
import br.com.painel.core.model.PaginaRegistros;
import br.com.painel.core.resource.security.CredenciaisRequisicao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping({
    "/api/empresas/{empresaId}/candidatos",
    "/api/integracoes/empresas/{empresaId}/candidatos"
})
public class CandidatoResource {
    private final CandidatoService registros;

    public CandidatoResource(CandidatoService registros) {
        this.registros = registros;
    }

    @GetMapping
    public PaginaRegistros<Candidato> listar(
            @PathVariable UUID empresaId,
            @RequestParam(defaultValue = "") @Size(max = 160) String busca,
            @RequestParam(defaultValue = "false") boolean incluirArquivados,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
            HttpServletRequest requisicao,
            Principal usuario) {
        return registros.listar(
                empresaId,
                CredenciaisRequisicao.obter(requisicao, usuario),
                busca,
                incluirArquivados,
                pagina,
                tamanho);
    }

    @GetMapping("/{id}")
    public Candidato consultar(
            @PathVariable UUID empresaId,
            @PathVariable UUID id,
            HttpServletRequest requisicao,
            Principal usuario) {
        return registros.consultar(empresaId, id, CredenciaisRequisicao.obter(requisicao, usuario));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Candidato criar(
            @PathVariable UUID empresaId,
            @Valid @RequestBody DadosCandidato dados,
            HttpServletRequest requisicao,
            Principal usuario) {
        return registros.criar(empresaId, dados, CredenciaisRequisicao.obter(requisicao, usuario));
    }

    @PutMapping("/{id}")
    public Candidato atualizar(
            @PathVariable UUID empresaId,
            @PathVariable UUID id,
            @Valid @RequestBody DadosCandidato dados,
            HttpServletRequest requisicao,
            Principal usuario) {
        return registros.atualizar(
                empresaId, id, dados, CredenciaisRequisicao.obter(requisicao, usuario));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arquivar(
            @PathVariable UUID empresaId,
            @PathVariable UUID id,
            @RequestParam @Min(0) long revisao,
            HttpServletRequest requisicao,
            Principal usuario) {
        registros.arquivar(
                empresaId, id, revisao, CredenciaisRequisicao.obter(requisicao, usuario));
    }
}
