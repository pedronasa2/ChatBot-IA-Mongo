package br.com.painel.modulos;

import static br.com.painel.persistencia.Documentos.q;

import br.com.painel.empresas.AcessoEmpresa;
import br.com.painel.persistencia.Documentos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcessoModulos {
  public enum Modulo {
    CANDIDATOS,
    AGENDA
  }

  public record Credenciais(String emailUsuario, String tokenIntegracao) {
    public static Credenciais usuario(Principal usuario) {
      return new Credenciais(usuario.getName(), null);
    }

    public static Credenciais integracao(String token) {
      return new Credenciais(null, token);
    }
  }

  private final Documentos documentos;
  private final AcessoEmpresa empresas;
  private final String tokenEsperado;

  public AcessoModulos(
      Documentos documentos,
      AcessoEmpresa empresas,
      @Value("${painel.integracao.token-modulos:}") String tokenEsperado) {
    this.documentos = documentos;
    this.empresas = empresas;
    this.tokenEsperado = tokenEsperado;
  }

  public void exigir(UUID empresaId, Modulo modulo, Credenciais credenciais) {
    if (credenciais.emailUsuario() != null) {
      empresas.exigirVinculo(empresaId, credenciais.emailUsuario());
    } else {
      if (tokenEsperado.isBlank())
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "Integração dos módulos não configurada.");
      String token = credenciais.tokenIntegracao() == null ? "" : credenciais.tokenIntegracao();
      if (!MessageDigest.isEqual(
          tokenEsperado.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Credencial da integração inválida.");
      }
    }
    var empresa = documentos.um("empresas", q("_id", empresaId));
    if (empresa == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada.");
    String campo =
        modulo == Modulo.CANDIDATOS ? "modulo_candidatos_habilitado" : "modulo_agenda_habilitado";
    if (!empresa.getBoolean(campo, false))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Módulo desabilitado para esta empresa.");
    // Escrita na mesma transação cria conflito com uma desativação concorrente.
    documentos.mongo.updateFirst(
        q("_id", empresaId).addCriteria(Criteria.where(campo).is(true)),
        new Update().inc("controle_acesso_modulos", 1),
        "empresas");
  }
}
