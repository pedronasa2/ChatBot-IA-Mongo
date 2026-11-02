package br.com.painel.modulos;

import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ControladorCurriculos {
  private final ServicoCandidatos candidatos;

  public ControladorCurriculos(ServicoCandidatos candidatos) {
    this.candidatos = candidatos;
  }

  @GetMapping("/api/empresas/{empresaId}/candidatos/{id}/curriculo")
  public ResponseEntity<Void> abrir(
      @PathVariable UUID empresaId, @PathVariable UUID id, Principal usuario) {
    var candidato = candidatos.consultar(empresaId, id, AcessoModulos.Credenciais.usuario(usuario));
    if (candidato.urlCurriculo().isBlank())
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Este candidato não tem um currículo vinculado.");
    ValidacaoRegistros.urlCurriculo(candidato.urlCurriculo());
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(candidato.urlCurriculo()))
        .build();
  }
}
