package br.com.painel.core.resource;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ErrosApi {
    @ExceptionHandler(br.com.painel.core.model.exception.NegocioException.class)
    ResponseEntity<?> negocio(br.com.painel.core.model.exception.NegocioException exception) {
        int status =
                switch (exception.getErro()) {
                    case DADOS_INVALIDOS -> 400;
                    case NAO_AUTORIZADO -> 401;
                    case ACESSO_NEGADO -> 403;
                    case NAO_ENCONTRADO -> 404;
                    case CONFLITO -> 409;
                    case INTEGRACAO_INDISPONIVEL -> 503;
                };
        return ResponseEntity.status(status).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(
                        Map.of(
                                "mensagem",
                                e.getReason() == null
                                        ? "Não foi possível concluir a operação."
                                        : e.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid() {
        return ResponseEntity.badRequest()
                .body(Map.of("mensagem", "Revise os campos obrigatórios e os limites de tamanho."));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<?> conflict() {
        return ResponseEntity.status(409)
                .body(
                        Map.of(
                                "mensagem",
                                "Dados alterados em outra sessão. Recarregue antes de salvar."));
    }
}
