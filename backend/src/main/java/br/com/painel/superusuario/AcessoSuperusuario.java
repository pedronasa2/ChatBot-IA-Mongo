package br.com.painel.superusuario;

import static br.com.painel.persistencia.Documentos.q;

import br.com.painel.persistencia.Documentos;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcessoSuperusuario {
  private final Documentos db;

  public AcessoSuperusuario(Documentos db) {
    this.db = db;
  }

  public boolean permitido(String email) {
    return usuario(email) != null;
  }

  private org.bson.Document usuario(String email) {
    return db.um(
        "usuarios",
        q("email", email)
            .addCriteria(Criteria.where("ativo").is(true))
            .addCriteria(Criteria.where("superusuario").is(true)));
  }

  public UUID exigir(Principal p) {
    var u = usuario(p.getName());
    if (u == null)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo de superusuário.");
    return u.get("_id", UUID.class);
  }
}
