package br.com.painel.superusuario;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/integracoes/mensagens")
public class ControladorIntegracaoMetricas {
  private final ServicoRegistroMensagens mensagens;
  private final String tokenEsperado;

  public ControladorIntegracaoMetricas(ServicoRegistroMensagens mensagens,
      @Value("${painel.integracao.token-metricas:}") String tokenEsperado) {
    this.mensagens = mensagens;
    this.tokenEsperado = tokenEsperado;
  }

  @PostMapping
  public ResponseEntity<Map<String, Boolean>> registrar(
      @RequestHeader(value = "X-Token-Metricas", defaultValue = "") String token,
      @Valid @RequestBody ServicoRegistroMensagens.EventoMensagem evento) {
    if (tokenEsperado.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "A coleta de métricas ainda não foi configurada.");
    }
    if (!MessageDigest.isEqual(tokenEsperado.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial da integração inválida.");
    }
    boolean registrado = mensagens.registrar(evento);
    return ResponseEntity.status(registrado ? HttpStatus.CREATED : HttpStatus.OK)
        .body(Map.of("registrado", registrado));
  }
}
