package br.com.painel.configuracoes;

import br.com.painel.empresas.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/empresas")
public class ControladorConfiguracoes {
  private final AcessoEmpresa acesso;
  private final RepositorioEmpresa empresas;
  private final RepositorioConfiguracaoBot configuracoesBot;
  private final JdbcTemplate jdbc;

  public ControladorConfiguracoes(
      AcessoEmpresa a, RepositorioEmpresa c, RepositorioConfiguracaoBot d, JdbcTemplate j) {
    acesso = a;
    empresas = c;
    configuracoesBot = d;
    jdbc = j;
  }

  public record DadosEmpresa(
      @NotBlank @Size(max = 160) String nome,
      @NotNull @Size(max = 5000) String descricao,
      @NotNull @Size(max = 500) String endereco,
      @NotNull @Size(max = 300) String contato,
      @NotNull @Size(max = 500) String site,
      @NotNull @Size(max = 10000) String servicos,
      @NotBlank @Size(max = 80) String fusoHorario,
      @Min(0) long revisao) {}

  public record DadosRascunhoBot(
      @NotBlank @Size(max = 80) String nomeBot,
      @Pattern(regexp = "cordial|formal|descontraido") @NotNull String tomVoz,
      boolean usarEmojis,
      @NotNull @Size(max = 10000) String instrucoes,
      @NotNull @Size(max = 5000) String instrucoesTransferencia,
      @Min(0) long revisao) {}

  @GetMapping
  public List<Map<String, Object>> listar(Principal p) {
    return acesso.listar(p.getName());
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public Empresa empresa(@PathVariable UUID id, Principal p) {
    acesso.exigirVinculo(id, p.getName());
    return empresas.findById(id).orElseThrow();
  }

  @PutMapping("/{id}")
  @Transactional
  public Empresa salvarEmpresa(
      @PathVariable UUID id, @Valid @RequestBody DadosEmpresa b, Principal p) {
    UUID usuarioResponsavelId = acesso.exigirVinculo(id, p.getName());
    try {
      ZoneId.of(b.fusoHorario());
    } catch (DateTimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fuso horário inválido");
    }
    if (!b.site().isBlank()) {
      try {
        var uri = java.net.URI.create(b.site());
        if (!Set.of("https", "http").contains(uri.getScheme())
            || uri.getHost() == null
            || uri.getUserInfo() != null) throw new IllegalArgumentException();
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Informe um site com http:// ou https://");
      }
    }
    Empresa c = empresas.findById(id).orElseThrow();
    if (c.revisao != b.revisao())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Dados alterados em outra sessão. Recarregue antes de salvar.");
    c.nome = b.nome().strip();
    c.descricao = b.descricao();
    c.endereco = b.endereco();
    c.contato = b.contato();
    c.site = b.site();
    c.servicos = b.servicos();
    c.fusoHorario = b.fusoHorario();
    c.atualizadoEm = Instant.now();
    empresas.saveAndFlush(c);
    registrarAuditoria(id, usuarioResponsavelId, null, "dados_empresa_salvos");
    return c;
  }

  @GetMapping("/{id}/rascunho")
  @Transactional(readOnly = true)
  public ConfiguracaoBot rascunho(@PathVariable UUID id, Principal p) {
    acesso.exigirVinculo(id, p.getName());
    return configuracoesBot
        .findByEmpresaIdAndEstado(id, "rascunho")
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rascunho não encontrado"));
  }

  @PutMapping("/{id}/rascunho")
  @Transactional
  public ConfiguracaoBot salvarRascunho(@PathVariable UUID id, @Valid @RequestBody DadosRascunhoBot b, Principal p) {
    UUID usuarioResponsavelId = acesso.exigirVinculo(id, p.getName());
    ConfiguracaoBot d = configuracoesBot.findByEmpresaIdAndEstado(id, "rascunho").orElseThrow();
    if (d.revisao != b.revisao())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Rascunho alterado em outra sessão. Recarregue antes de salvar.");
    d.nomeBot = b.nomeBot().strip();
    d.tomVoz = b.tomVoz();
    d.usarEmojis = b.usarEmojis();
    d.instrucoes = b.instrucoes();
    d.instrucoesTransferencia = b.instrucoesTransferencia();
    d.autorId = usuarioResponsavelId;
    d.atualizadoEm = Instant.now();
    configuracoesBot.saveAndFlush(d);
    registrarAuditoria(id, usuarioResponsavelId, d.id, "rascunho_salvo");
    return d;
  }

  private void registrarAuditoria(UUID c, UUID a, UUID d, String acao) {
    jdbc.update(
        "insert into eventos_auditoria(id,empresa_id,usuario_responsavel_id,configuracao_bot_id,acao)"
            + " values(?,?,?,?,?)",
        UUID.randomUUID(),
        c,
        a,
        d,
        acao);
  }
}
