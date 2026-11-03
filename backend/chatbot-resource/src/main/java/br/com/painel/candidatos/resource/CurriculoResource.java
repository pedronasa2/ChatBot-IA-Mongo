package br.com.painel.candidatos.resource;

import br.com.painel.candidatos.service.CandidatoService;
import br.com.painel.candidatos.service.CandidatoValidator;
import br.com.painel.core.model.security.Credenciais;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.security.Principal;
import java.util.UUID;

@RestController
public class CurriculoResource {
    private final CandidatoService candidatos;

    public CurriculoResource(CandidatoService candidatos) {
        this.candidatos = candidatos;
    }

    @GetMapping("/api/empresas/{empresaId}/candidatos/{id}/curriculo")
    public ResponseEntity<Void> abrir(
            @PathVariable UUID empresaId, @PathVariable UUID id, Principal usuario) {
        var candidato = candidatos.consultar(empresaId, id, Credenciais.usuario(usuario));
        if (candidato.urlCurriculo().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Este candidato não tem um currículo vinculado.");
        }
        CandidatoValidator.urlCurriculo(candidato.urlCurriculo());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(candidato.urlCurriculo()))
                .build();
    }
}
