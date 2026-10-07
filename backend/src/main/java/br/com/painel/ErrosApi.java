package br.com.painel;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErrosApi {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            Map.of(
                "mensagem",
                e.getReason() == null ? "Não foi possível concluir a operação." : e.getReason()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> invalid() {
    return ResponseEntity.badRequest()
        .body(Map.of("mensagem", "Revise os campos obrigatórios e os limites de tamanho."));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(409)
        .body(Map.of("mensagem", "Dados alterados em outra sessão. Recarregue antes de salvar."));
  }
}
