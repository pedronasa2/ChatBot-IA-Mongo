package br.com.painel.superusuario;

import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicoRegistroMensagens {
  private final JdbcTemplate jdbc;

  public ServicoRegistroMensagens(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O evento não pode ter uma data futura.");
    }
    if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from empresas where id=?)", Boolean.class, evento.empresaId()))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada.");
    }
    return jdbc.update(
        "insert into eventos_mensagens_atendimento(id,empresa_id,origem,id_evento_origem,direcao,resultado,ocorrido_em) "
            + "values(?,?,?,?,?,?,?) on conflict(origem,id_evento_origem) do nothing",
        UUID.randomUUID(), evento.empresaId(), evento.origem().strip(), evento.idEventoOrigem().strip(),
        evento.direcao(), evento.resultado(), Timestamp.from(evento.ocorridoEm())) == 1;
  }
}
