package br.com.painel.superusuario;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicoPromptGlobal {
  private final JdbcTemplate jdbc;

  public ServicoPromptGlobal(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record DadosPromptGlobal(
      @NotBlank @Size(max = 40000) String promptSistema,
      @AssertTrue boolean complementarConfiguracoesEmpresa,
      @Min(0) long revisao) {}

  public record PromptGlobal(
      String promptSistema, boolean complementarConfiguracoesEmpresa, long revisao,
      Instant atualizadoEm, String atualizadoPorEmail) {}

  public record VersaoPromptGlobal(
      String promptSistema, boolean complementarConfiguracoesEmpresa, long revisao,
      Instant criadoEm, String usuarioResponsavelEmail) {}

  public PromptGlobal consultar() {
    return jdbc.queryForObject(
        "select g.*,u.email from configuracoes_globais g left join usuarios u on u.id=g.atualizado_por where g.id=1",
        (registro, indice) -> new PromptGlobal(
            registro.getString("prompt_sistema"), registro.getBoolean("complementar_configuracoes_empresa"),
            registro.getLong("revisao"), registro.getTimestamp("atualizado_em").toInstant(),
            registro.getString("email")));
  }

  @Transactional
  public PromptGlobal salvar(DadosPromptGlobal dados, UUID usuarioId) {
    String prompt = dados.promptSistema().strip();
    int alterados = jdbc.update(
        "update configuracoes_globais set prompt_sistema=?,complementar_configuracoes_empresa=?,"
            + "revisao=revisao+1,atualizado_por=?,atualizado_em=now() where id=1 and revisao=?",
        prompt, dados.complementarConfiguracoesEmpresa(), usuarioId, dados.revisao());
    if (alterados != 1) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "O prompt global foi alterado em outra sessão. Recarregue antes de salvar.");
    }
    jdbc.update(
        "insert into historico_prompts_globais(id,prompt_sistema,complementar_configuracoes_empresa,revisao,usuario_responsavel_id) values(?,?,?,?,?)",
        UUID.randomUUID(), prompt, dados.complementarConfiguracoesEmpresa(), dados.revisao() + 1, usuarioId);
    return consultar();
  }

  public List<VersaoPromptGlobal> historico() {
    return jdbc.query(
        "select h.*,u.email from historico_prompts_globais h join usuarios u on u.id=h.usuario_responsavel_id order by h.revisao desc limit 20",
        (registro, indice) -> new VersaoPromptGlobal(
            registro.getString("prompt_sistema"), registro.getBoolean("complementar_configuracoes_empresa"),
            registro.getLong("revisao"), registro.getTimestamp("criado_em").toInstant(),
            registro.getString("email")));
  }

  public record PreviaPrompt(String nomeEmpresa, String promptComposto) {}

  @Transactional(readOnly = true)
  public PreviaPrompt visualizarRascunho(UUID empresaId) {
    var empresas = jdbc.queryForList(
        "select e.*,b.nome_bot,b.tom_voz,b.usar_emojis,b.instrucoes,b.instrucoes_transferencia "
            + "from empresas e join versoes_configuracoes_bot b on b.empresa_id=e.id "
            + "where e.id=? and b.estado='rascunho'", empresaId);
    if (empresas.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa ou rascunho não encontrado.");
    }
    var empresa = empresas.getFirst();
    var global = consultar();
    var texto = new StringBuilder(global.promptSistema());
    if (global.complementarConfiguracoesEmpresa()) {
      texto.append("\n\nCONFIGURAÇÃO COMPLEMENTAR DA EMPRESA\n")
          .append("As regras globais acima têm prioridade sobre estas configurações.\n");
      for (var campo : List.of(
          Map.entry("nome", "Empresa"), Map.entry("descricao", "Descrição do negócio"),
          Map.entry("endereco", "Endereço"), Map.entry("contato", "Contatos"), Map.entry("site", "Site"),
          Map.entry("servicos", "Serviços ou produtos"), Map.entry("fuso_horario", "Fuso horário"),
          Map.entry("nome_bot", "Nome do assistente"), Map.entry("tom_voz", "Tom de voz"),
          Map.entry("usar_emojis", "Permitir emojis"), Map.entry("instrucoes", "Instruções adicionais"),
          Map.entry("instrucoes_transferencia", "Transferência para uma pessoa"))) {
        Object valor = empresa.get(campo.getKey());
        if (campo.getKey().equals("usar_emojis")) valor = Boolean.TRUE.equals(valor) ? "sim" : "não";
        texto.append(campo.getValue()).append(": ").append(valor).append('\n');
      }
    }
    return new PreviaPrompt(empresa.get("nome").toString(), texto.toString().strip());
  }
}
