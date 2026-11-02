package br.com.painel.superusuario;

import static br.com.painel.persistencia.Documentos.q;

import br.com.painel.persistencia.Documentos;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicoRegistroMensagens {
  private final Documentos documentos;

  public ServicoRegistroMensagens(Documentos documentos) {
    this.documentos = documentos;
  }

  public record EventoMensagem(
      @NotNull UUID empresaId,
      @NotBlank @Size(max = 80) String origem,
      @NotBlank @Size(max = 160) String idEventoOrigem,
      @NotNull @Pattern(regexp = "recebida|enviada") String direcao,
      @NotNull @Pattern(regexp = "sucesso|erro") String resultado,
      @NotNull Instant ocorridoEm) {}

  public boolean registrar(EventoMensagem evento) {
    if (evento.ocorridoEm().isAfter(Instant.now().plusSeconds(5))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O evento não pode ter uma data futura.");
    }
    if (documentos.um("empresas", q("_id", evento.empresaId())) == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada.");
    try {
      documentos.inserir(
          "eventos_mensagens_atendimento",
          new Document("_id", UUID.randomUUID())
              .append("empresa_id", evento.empresaId())
              .append("origem", evento.origem().strip())
              .append("id_evento_origem", evento.idEventoOrigem().strip())
              .append("direcao", evento.direcao())
              .append("resultado", evento.resultado())
              .append("ocorrido_em", Date.from(evento.ocorridoEm()))
              .append("registrado_em", new Date()));
      return true;
    } catch (DuplicateKeyException e) {
      return false;
    }
  }
}
