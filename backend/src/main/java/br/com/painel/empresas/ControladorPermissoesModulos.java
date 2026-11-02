package br.com.painel.empresas;

import static br.com.painel.persistencia.Documentos.q;

import br.com.painel.persistencia.Documentos;
import br.com.painel.superusuario.AcessoSuperusuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/superusuario/empresas/{empresaId}/modulos")
public class ControladorPermissoesModulos {
  public record PermissoesModulos(
      @NotNull Boolean moduloCandidatosHabilitado,
      @NotNull Boolean moduloAgendaHabilitado,
      @Min(0) long revisao) {}

  private final Documentos documentos;
  private final AcessoSuperusuario acesso;
  private final RepositorioEmpresa empresas;

  public ControladorPermissoesModulos(
      Documentos documentos, AcessoSuperusuario acesso, RepositorioEmpresa empresas) {
    this.documentos = documentos;
    this.acesso = acesso;
    this.empresas = empresas;
  }

  @PutMapping
  @Transactional
  public Empresa atualizar(
      @PathVariable UUID empresaId,
      @Valid @RequestBody PermissoesModulos dados,
      Principal usuario) {
    UUID responsavel = acesso.exigir(usuario);
    var resultado =
        documentos.mongo.updateFirst(
            q("_id", empresaId).addCriteria(Criteria.where("revisao").is(dados.revisao())),
            new Update()
                .set("modulo_candidatos_habilitado", dados.moduloCandidatosHabilitado())
                .set("modulo_agenda_habilitado", dados.moduloAgendaHabilitado())
                .inc("revisao", 1)
                .set("atualizado_em", new java.util.Date()),
            "empresas");
    if (resultado.getModifiedCount() != 1) {
      if (documentos.um("empresas", q("_id", empresaId)) == null)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada.");
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Empresa alterada em outra sessão. Recarregue antes de salvar as permissões.");
    }
    documentos.auditar(empresaId, responsavel, null, "permissoes_modulos_atualizadas");
    return empresas.findById(empresaId).orElseThrow();
  }
}
