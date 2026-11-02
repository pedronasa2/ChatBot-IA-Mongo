package br.com.painel.modulos;

import java.net.URI;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ValidacaoRegistros {
  private ValidacaoRegistros() {}

  static void urlCurriculo(String endereco) {
    if (endereco.isBlank()) return;
    try {
      URI uri = URI.create(endereco);
      if (!Set.of("http", "https").contains(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null) {
        throw new IllegalArgumentException();
      }
    } catch (IllegalArgumentException erro) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe um endereço de currículo com http:// ou https://.");
    }
  }

  static String busca(String texto) {
    return "%" + texto.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }
}
