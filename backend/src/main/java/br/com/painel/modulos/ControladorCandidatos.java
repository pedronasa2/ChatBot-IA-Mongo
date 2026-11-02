package br.com.painel.modulos;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({
  "/api/empresas/{empresaId}/candidatos",
  "/api/integracoes/empresas/{empresaId}/candidatos"
})
public class ControladorCandidatos {
  private final ServicoCandidatos registros;

  public ControladorCandidatos(ServicoCandidatos registros) {
    this.registros = registros;
  }

  @GetMapping
  public PaginaRegistros<ServicoCandidatos.Candidato> listar(
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
  public ServicoCandidatos.Candidato consultar(
      @PathVariable UUID empresaId,
      @PathVariable UUID id,
      HttpServletRequest requisicao,
      Principal usuario) {
    return registros.consultar(empresaId, id, CredenciaisRequisicao.obter(requisicao, usuario));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ServicoCandidatos.Candidato criar(
      @PathVariable UUID empresaId,
      @Valid @RequestBody ServicoCandidatos.DadosCandidato dados,
      HttpServletRequest requisicao,
      Principal usuario) {
    return registros.criar(empresaId, dados, CredenciaisRequisicao.obter(requisicao, usuario));
  }

  @PutMapping("/{id}")
  public ServicoCandidatos.Candidato atualizar(
      @PathVariable UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody ServicoCandidatos.DadosCandidato dados,
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
    registros.arquivar(empresaId, id, revisao, CredenciaisRequisicao.obter(requisicao, usuario));
  }
}
