package br.com.painel.superusuario;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcessoSuperusuario {
  private final JdbcTemplate jdbc;

  public AcessoSuperusuario(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean permitido(String email) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "select exists(select 1 from usuarios where email=? and ativo and superusuario)",
        Boolean.class, email));
  }

  public UUID exigir(Principal usuario) {
    // Consulta a permissão atual, inclusive para sessões persistentes já abertas.
    var ids = jdbc.query(
        "select id from usuarios where email=? and ativo and superusuario",
        (registro, indice) -> registro.getObject(1, UUID.class), usuario.getName());
    if (ids.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de superusuário.");
    }
    return ids.getFirst();
  }
}
