package br.com.painel.modulos;

import static br.com.painel.persistencia.Documentos.*;

import br.com.painel.modulos.AcessoModulos.Credenciais;
import br.com.painel.persistencia.Documentos;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicoAgenda {
  public record DadosAgendamento(
      @NotBlank @Size(max = 160) String nomePessoa,
      @NotNull @Email @Size(max = 254) String email,
      @NotNull @Size(max = 40) String telefone,
      @NotBlank @Size(max = 200) String assunto,
      @NotNull @Size(max = 160) String responsavel,
      @NotNull Instant inicio,
      @NotNull Instant fim,
      @NotNull @Pattern(regexp = "agendado|confirmado|concluido|cancelado") String situacao,
      @NotNull @Size(max = 5000) String observacoes,
      boolean arquivado,
      @Min(0) long revisao) {}

  public record Agendamento(
      UUID id,
      UUID empresaId,
      String nomePessoa,
      String email,
      String telefone,
      String assunto,
      String responsavel,
      Instant inicio,
      Instant fim,
      String situacao,
      String observacoes,
      boolean arquivado,
      long revisao,
      Instant criadoEm,
      Instant atualizadoEm) {}

  private final Documentos db;
  private final AcessoModulos acesso;

  public ServicoAgenda(Documentos db, AcessoModulos acesso) {
    this.db = db;
    this.acesso = acesso;
  }

  public PaginaRegistros<Agendamento> listar(
      UUID empresaId,
      Credenciais credenciais,
      String busca,
      boolean incluirArquivados,
      Instant de,
      Instant ate,
      int pagina,
      int tamanho) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.AGENDA, credenciais);
    Query consulta = q("empresa_id", empresaId);
    if (!incluirArquivados) consulta.addCriteria(Criteria.where("arquivado").is(false));
    String termo = busca == null ? "" : busca.strip();
    if (!termo.isEmpty())
      consulta.addCriteria(
          new Criteria()
              .orOperator(
                  Criteria.where("nome_pessoa").regex(java.util.regex.Pattern.quote(termo), "i"),
                  Criteria.where("assunto").regex(java.util.regex.Pattern.quote(termo), "i"),
                  Criteria.where("responsavel").regex(java.util.regex.Pattern.quote(termo), "i")));
    if (de != null && ate != null && !ate.isAfter(de))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período de busca inválido.");
    if (de != null) consulta.addCriteria(Criteria.where("fim").gt(Date.from(de)));
    if (ate != null) consulta.addCriteria(Criteria.where("inicio").lt(Date.from(ate)));
    long total = db.contar("agendamentos_atendimento", consulta);
    var registros =
        db
            .lista(
                "agendamentos_atendimento",
                consulta
                    .with(Sort.by("inicio", "_id"))
                    .skip((long) pagina * tamanho)
                    .limit(tamanho))
            .stream()
            .map(this::mapear)
            .toList();
    return new PaginaRegistros<>(registros, total, pagina, tamanho);
  }

  public Agendamento consultar(UUID empresaId, UUID id, Credenciais credenciais) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.AGENDA, credenciais);
    return encontrar(empresaId, id);
  }

  private void validar(DadosAgendamento dados) {
    if (!dados.fim().isAfter(dados.inicio()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O fim do atendimento deve ser posterior ao início.");
  }

  private Update alteracoes(DadosAgendamento dados) {
    return new Update()
        .set("nome_pessoa", dados.nomePessoa().strip())
        .set("email", dados.email().strip())
        .set("telefone", dados.telefone().strip())
        .set("assunto", dados.assunto().strip())
        .set("responsavel", dados.responsavel().strip())
        .set("inicio", Date.from(dados.inicio()))
        .set("fim", Date.from(dados.fim()))
        .set("situacao", dados.situacao())
        .set("observacoes", dados.observacoes())
        .set("arquivado", dados.arquivado())
        .set("atualizado_em", new Date());
  }

  public Agendamento criar(UUID empresaId, DadosAgendamento dados, Credenciais credenciais) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.AGENDA, credenciais);
    validar(dados);
    UUID id = UUID.randomUUID();
    Document doc =
        new Document("_id", id)
            .append("empresa_id", empresaId)
            .append("revisao", 0L)
            .append("criado_em", new Date());
    doc.putAll((Document) alteracoes(dados).getUpdateObject().get("$set"));
    db.inserir("agendamentos_atendimento", doc);
    return mapear(doc);
  }

  public Agendamento atualizar(
      UUID empresaId, UUID id, DadosAgendamento dados, Credenciais credenciais) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.AGENDA, credenciais);
    validar(dados);
    encontrar(empresaId, id);
    var r =
        db.mongo.updateFirst(
            q("_id", id)
                .addCriteria(Criteria.where("empresa_id").is(empresaId))
                .addCriteria(Criteria.where("revisao").is(dados.revisao())),
            alteracoes(dados).inc("revisao", 1),
            "agendamentos_atendimento");
    exigirRevisao(r.getModifiedCount());
    return encontrar(empresaId, id);
  }

  public void arquivar(UUID empresaId, UUID id, long revisao, Credenciais credenciais) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.AGENDA, credenciais);
    encontrar(empresaId, id);
    exigirRevisao(
        db.mongo
            .updateFirst(
                q("_id", id)
                    .addCriteria(Criteria.where("empresa_id").is(empresaId))
                    .addCriteria(Criteria.where("revisao").is(revisao)),
                new Update()
                    .set("arquivado", true)
                    .inc("revisao", 1)
                    .set("atualizado_em", new Date()),
                "agendamentos_atendimento")
            .getModifiedCount());
  }

  private Agendamento encontrar(UUID empresaId, UUID id) {
    var doc =
        db.um(
            "agendamentos_atendimento",
            q("_id", id).addCriteria(Criteria.where("empresa_id").is(empresaId)));
    if (doc == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado.");
    return mapear(doc);
  }

  private void exigirRevisao(long alterados) {
    if (alterados != 1)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Registro alterado em outra sessão. Recarregue antes de salvar.");
  }

  private Agendamento mapear(Document d) {
    return new Agendamento(
        d.get("_id", UUID.class),
        d.get("empresa_id", UUID.class),
        d.getString("nome_pessoa"),
        d.getString("email"),
        d.getString("telefone"),
        d.getString("assunto"),
        d.getString("responsavel"),
        instante(d, "inicio"),
        instante(d, "fim"),
        d.getString("situacao"),
        d.getString("observacoes"),
        d.getBoolean("arquivado", false),
        numero(d, "revisao"),
        instante(d, "criado_em"),
        instante(d, "atualizado_em"));
  }
}
