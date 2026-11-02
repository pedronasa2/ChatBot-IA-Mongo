package br.com.painel.empresas;

import static br.com.painel.persistencia.Documentos.q;

import br.com.painel.persistencia.Documentos;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcessoEmpresa {
  private final Documentos db;

  public AcessoEmpresa(Documentos db) {
    this.db = db;
  }

  public UUID exigirVinculo(UUID empresa, String email) {
    var u = db.um("usuarios", q("email", email).addCriteria(Criteria.where("ativo").is(true)));
    if (u == null
        || db.um("empresas", q("_id", empresa)) == null
        || (!u.getBoolean("superusuario", false)
            && db.um(
                    "vinculos_usuarios_empresas",
                    q("usuario_id", u.get("_id"))
                        .addCriteria(Criteria.where("empresa_id").is(empresa)))
                == null))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada");
    return u.get("_id", UUID.class);
  }

  public List<Map<String, Object>> listar(String email) {
    var u = db.um("usuarios", q("email", email).addCriteria(Criteria.where("ativo").is(true)));
    if (u == null) return List.of();
    var vinculos = db.lista("vinculos_usuarios_empresas", q("usuario_id", u.get("_id")));
    Query consulta = new Query();
    if (!u.getBoolean("superusuario", false))
      consulta.addCriteria(
          Criteria.where("_id").in(vinculos.stream().map(v -> v.get("empresa_id")).toList()));
    return db.lista("empresas", consulta.with(Sort.by("nome", "_id"))).stream()
        .map(
            e ->
                Map.<String, Object>of(
                    "id",
                    e.get("_id"),
                    "nome",
                    e.getString("nome"),
                    "papel",
                    u.getBoolean("superusuario", false)
                        ? "superusuario"
                        : vinculos.stream()
                            .filter(v -> v.get("empresa_id").equals(e.get("_id")))
                            .findFirst()
                            .orElseThrow()
                            .getString("papel")))
        .toList();
  }
}
