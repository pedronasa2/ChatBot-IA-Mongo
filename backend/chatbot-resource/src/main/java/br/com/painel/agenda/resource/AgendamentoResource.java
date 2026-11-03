package br.com.painel.agenda.resource;

import br.com.painel.agenda.model.vo.Agendamento;
import br.com.painel.agenda.model.vo.DadosAgendamento;
import br.com.painel.agenda.service.AgendamentoService;
import br.com.painel.core.model.PaginaRegistros;
import br.com.painel.core.resource.security.CredenciaisRequisicao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping({
    "/api/empresas/{empresaId}/agenda",
    "/api/integracoes/empresas/{empresaId}/agenda"
})
public class AgendamentoResource {
    private final AgendamentoService registros;

    public AgendamentoResource(AgendamentoService registros) {
        this.registros = registros;
    }

    @GetMapping
    public PaginaRegistros<Agendamento> listar(
            @PathVariable UUID empresaId,
            @RequestParam(defaultValue = "") @Size(max = 160) String busca,
            @RequestParam(defaultValue = "false") boolean incluirArquivados,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
            @RequestParam(required = false) Instant de,
            @RequestParam(required = false) Instant ate,
            HttpServletRequest requisicao,
            Principal usuario) {
        return registros.listar(
                empresaId,
                CredenciaisRequisicao.obter(requisicao, usuario),
                busca,
                incluirArquivados,
                de,
                ate,
                pagina,
                tamanho);
    }

    @GetMapping("/{id}")
    public Agendamento consultar(
            @PathVariable UUID empresaId,
            @PathVariable UUID id,
            HttpServletRequest requisicao,
            Principal usuario) {
        return registros.consultar(empresaId, id, CredenciaisRequisicao.obter(requisicao, usuario));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agendamento criar(
            @PathVariable UUID empresaId,
            @Valid @RequestBody DadosAgendamento dados,
            HttpServletRequest requisicao,
            Principal usuario) {
        return registros.criar(empresaId, dados, CredenciaisRequisicao.obter(requisicao, usuario));
    }

    @PutMapping("/{id}")
    public Agendamento atualizar(
            @PathVariable UUID empresaId,
            @PathVariable UUID id,
            @Valid @RequestBody DadosAgendamento dados,
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
