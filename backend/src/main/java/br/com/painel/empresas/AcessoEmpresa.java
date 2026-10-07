package br.com.painel.empresas;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcessoEmpresa {
  private final JdbcTemplate jdbc;

  public AcessoEmpresa(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public UUID exigirVinculo(UUID empresa, String email) {
    var ids =
        jdbc.query(
            "select u.id from usuarios u where u.email=? and u.ativo=true"
                + " and exists(select 1 from empresas c where c.id=? and (u.superusuario"
                + " or exists(select 1 from vinculos_usuarios_empresas m where m.usuario_id=u.id and m.empresa_id=c.id)))",
            (r, n) -> r.getObject(1, UUID.class),
            email,
            empresa);
    if (ids.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada");
    return ids.getFirst();
  }

  public List<Map<String, Object>> listar(String email) {
    return jdbc.queryForList(
        "select c.id,c.nome,case when u.superusuario then 'superusuario' else m.papel end as papel"
            + " from usuarios u cross join empresas c left join vinculos_usuarios_empresas m"
            + " on m.empresa_id=c.id and m.usuario_id=u.id"
            + " where u.email=? and u.ativo=true and (u.superusuario or m.id is not null) order by c.nome,c.id",
        email);
  }
}
