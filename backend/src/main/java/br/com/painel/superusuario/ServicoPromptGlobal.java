package br.com.painel.superusuario;

import static br.com.painel.persistencia.Documentos.*;

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
public class ServicoPromptGlobal {
  private final Documentos documentos;

  public ServicoPromptGlobal(Documentos documentos) {
    this.documentos = documentos;
  }

  public record DadosPromptGlobal(
      @NotBlank @Size(max = 40000) String promptSistema,
      @AssertTrue boolean complementarConfiguracoesEmpresa,
      @Min(0) long revisao) {}

  public record PromptGlobal(
      String promptSistema,
      boolean complementarConfiguracoesEmpresa,
      long revisao,
      Instant atualizadoEm,
      String atualizadoPorEmail) {}

  public record VersaoPromptGlobal(
      String promptSistema,
      boolean complementarConfiguracoesEmpresa,
      long revisao,
      Instant criadoEm,
      String usuarioResponsavelEmail) {}

  private String email(Object id) {
    var u = documentos.um("usuarios", q("_id", id));
    return u == null ? null : u.getString("email");
  }

  public PromptGlobal consultar() {
    var g = documentos.um("configuracoes_globais", q("_id", 1));
    return new PromptGlobal(
        g.getString("prompt_sistema"),
        g.getBoolean("complementar_configuracoes_empresa", true),
        numero(g, "revisao"),
        instante(g, "atualizado_em"),
        email(g.get("atualizado_por")));
  }

  @Transactional
  public PromptGlobal salvar(DadosPromptGlobal dados, UUID usuarioId) {
    String prompt = dados.promptSistema().strip();
    long alterados =
        documentos
            .mongo
            .updateFirst(
                q("_id", 1).addCriteria(Criteria.where("revisao").is(dados.revisao())),
                new Update()
                    .set("prompt_sistema", prompt)
                    .set(
                        "complementar_configuracoes_empresa",
                        dados.complementarConfiguracoesEmpresa())
                    .inc("revisao", 1)
                    .set("atualizado_por", usuarioId)
                    .set("atualizado_em", new java.util.Date()),
                "configuracoes_globais")
            .getModifiedCount();
    if (alterados != 1)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O prompt global foi alterado em outra sessão. Recarregue antes de salvar.");
    documentos.inserir(
        "historico_prompts_globais",
        new Document("_id", UUID.randomUUID())
            .append("prompt_sistema", prompt)
            .append("complementar_configuracoes_empresa", dados.complementarConfiguracoesEmpresa())
            .append("revisao", dados.revisao() + 1)
            .append("usuario_responsavel_id", usuarioId)
            .append("criado_em", new java.util.Date()));
    return consultar();
  }

  public List<VersaoPromptGlobal> historico() {
    return documentos
        .lista(
            "historico_prompts_globais",
            new Query().with(Sort.by(Sort.Direction.DESC, "revisao")).limit(20))
        .stream()
        .map(
            h ->
                new VersaoPromptGlobal(
                    h.getString("prompt_sistema"),
                    h.getBoolean("complementar_configuracoes_empresa", true),
                    numero(h, "revisao"),
                    instante(h, "criado_em"),
                    email(h.get("usuario_responsavel_id"))))
        .toList();
  }

  public record PreviaPrompt(String nomeEmpresa, String promptComposto) {}

  @Transactional(readOnly = true)
  public PreviaPrompt visualizarRascunho(UUID empresaId) {
    var empresa = documentos.um("empresas", q("_id", empresaId));
    var bot =
        documentos.um(
            "versoes_configuracoes_bot",
            q("empresa_id", empresaId).addCriteria(Criteria.where("estado").is("rascunho")));
    if (empresa == null || bot == null)
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Empresa ou rascunho não encontrado.");
    empresa.putAll(bot);
    var global = consultar();
    var texto = new StringBuilder(global.promptSistema());
    if (global.complementarConfiguracoesEmpresa()) {
      texto
          .append("\n\nCONFIGURAÇÃO COMPLEMENTAR DA EMPRESA\n")
          .append("As regras globais acima têm prioridade sobre estas configurações.\n");
      for (var campo :
          List.of(
              Map.entry("nome", "Empresa"),
              Map.entry("descricao", "Descrição do negócio"),
              Map.entry("endereco", "Endereço"),
              Map.entry("contato", "Contatos"),
              Map.entry("site", "Site"),
              Map.entry("servicos", "Serviços ou produtos"),
              Map.entry("fuso_horario", "Fuso horário"),
              Map.entry("nome_bot", "Nome do assistente"),
              Map.entry("tom_voz", "Tom de voz"),
              Map.entry("usar_emojis", "Permitir emojis"),
              Map.entry("instrucoes", "Instruções adicionais"),
              Map.entry("instrucoes_transferencia", "Transferência para uma pessoa"))) {
        Object valor = empresa.get(campo.getKey());
        if (campo.getKey().equals("usar_emojis"))
          valor = Boolean.TRUE.equals(valor) ? "sim" : "não";
        texto.append(campo.getValue()).append(": ").append(valor).append('\n');
      }
    }
    return new PreviaPrompt(empresa.get("nome").toString(), texto.toString().strip());
  }
}
