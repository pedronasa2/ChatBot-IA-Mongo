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
public class ServicoCandidatos {
  public record DadosCandidato(
      @NotBlank @Size(max = 160) String nomeCompleto,
      @NotNull @Email @Size(max = 254) String email,
      @NotNull @Size(max = 40) String telefone,
      @NotNull @Size(max = 160) String cargoPretendido,
      @NotNull @Size(max = 2000) String urlCurriculo,
      @NotNull @Size(max = 10000) String resumoProfissional,
      @NotNull @Size(max = 5000) String observacoes,
      @NotNull @Pattern(regexp = "novo|em_analise|aprovado|reprovado") String situacao,
      boolean arquivado,
      @Min(0) long revisao) {}

  public record Candidato(
      UUID id,
      UUID empresaId,
      String nomeCompleto,
      String email,
      String telefone,
      String cargoPretendido,
      String urlCurriculo,
      String resumoProfissional,
      String observacoes,
      String situacao,
      boolean arquivado,
      long revisao,
      Instant recebidoEm,
      Instant atualizadoEm) {}

  private final Documentos db;
  private final AcessoModulos acesso;

  public ServicoCandidatos(Documentos db, AcessoModulos acesso) {
    this.db = db;
    this.acesso = acesso;
  }

  public PaginaRegistros<Candidato> listar(
      UUID empresaId,
      Credenciais credenciais,
      String busca,
      boolean incluirArquivados,
      int pagina,
      int tamanho) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.CANDIDATOS, credenciais);
    Query consulta = q("empresa_id", empresaId);
    if (!incluirArquivados) consulta.addCriteria(Criteria.where("arquivado").is(false));
    String termo = busca == null ? "" : busca.strip();
    if (!termo.isEmpty())
      consulta.addCriteria(
          new Criteria()
              .orOperator(
                  Criteria.where("nome_completo").regex(java.util.regex.Pattern.quote(termo), "i"),
                  Criteria.where("email").regex(java.util.regex.Pattern.quote(termo), "i"),
                  Criteria.where("cargo_pretendido")
                      .regex(java.util.regex.Pattern.quote(termo), "i")));
    long total = db.contar("candidatos", consulta);
    var registros =
        db
            .lista(
                "candidatos",
                consulta
                    .with(Sort.by(Sort.Order.desc("recebido_em"), Sort.Order.asc("_id")))
                    .skip((long) pagina * tamanho)
                    .limit(tamanho))
            .stream()
            .map(this::mapear)
            .toList();
    return new PaginaRegistros<>(registros, total, pagina, tamanho);
  }

  public Candidato consultar(UUID empresaId, UUID id, Credenciais credenciais) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.CANDIDATOS, credenciais);
    return encontrar(empresaId, id);
  }

  private void validar(DadosCandidato dados) {
    ValidacaoRegistros.urlCurriculo(dados.urlCurriculo());
  }

  private Update alteracoes(DadosCandidato dados) {
    return new Update()
        .set("nome_completo", dados.nomeCompleto().strip())
        .set("email", dados.email().strip())
        .set("telefone", dados.telefone().strip())
        .set("cargo_pretendido", dados.cargoPretendido().strip())
        .set("url_curriculo", dados.urlCurriculo().strip())
        .set("resumo_profissional", dados.resumoProfissional())
        .set("observacoes", dados.observacoes())
        .set("situacao", dados.situacao())
        .set("arquivado", dados.arquivado())
        .set("atualizado_em", new Date());
  }

  public Candidato criar(UUID empresaId, DadosCandidato dados, Credenciais credenciais) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.CANDIDATOS, credenciais);
    validar(dados);
    UUID id = UUID.randomUUID();
    Document doc =
        new Document("_id", id)
            .append("empresa_id", empresaId)
            .append("revisao", 0L)
            .append("recebido_em", new Date());
    doc.putAll((Document) alteracoes(dados).getUpdateObject().get("$set"));
    db.inserir("candidatos", doc);
    return mapear(doc);
  }

  public Candidato atualizar(
      UUID empresaId, UUID id, DadosCandidato dados, Credenciais credenciais) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.CANDIDATOS, credenciais);
    validar(dados);
    encontrar(empresaId, id);
    var r =
        db.mongo.updateFirst(
            q("_id", id)
                .addCriteria(Criteria.where("empresa_id").is(empresaId))
                .addCriteria(Criteria.where("revisao").is(dados.revisao())),
            alteracoes(dados).inc("revisao", 1),
            "candidatos");
    exigirRevisao(r.getModifiedCount());
    return encontrar(empresaId, id);
  }

  public void arquivar(UUID empresaId, UUID id, long revisao, Credenciais credenciais) {
    acesso.exigir(empresaId, AcessoModulos.Modulo.CANDIDATOS, credenciais);
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
                "candidatos")
            .getModifiedCount());
  }

  private Candidato encontrar(UUID empresaId, UUID id) {
    var doc =
        db.um("candidatos", q("_id", id).addCriteria(Criteria.where("empresa_id").is(empresaId)));
    if (doc == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado.");
    return mapear(doc);
  }

  private void exigirRevisao(long alterados) {
    if (alterados != 1)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Registro alterado em outra sessão. Recarregue antes de salvar.");
  }

  private Candidato mapear(Document d) {
    return new Candidato(
        d.get("_id", UUID.class),
        d.get("empresa_id", UUID.class),
        d.getString("nome_completo"),
        d.getString("email"),
        d.getString("telefone"),
        d.getString("cargo_pretendido"),
        d.getString("url_curriculo"),
        d.getString("resumo_profissional"),
        d.getString("observacoes"),
        d.getString("situacao"),
        d.getBoolean("arquivado", false),
        numero(d, "revisao"),
        instante(d, "recebido_em"),
        instante(d, "atualizado_em"));
  }
}
