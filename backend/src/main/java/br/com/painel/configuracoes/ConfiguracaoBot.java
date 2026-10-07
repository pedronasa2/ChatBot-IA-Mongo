package br.com.painel.configuracoes;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "versoes_configuracoes_bot")
public class ConfiguracaoBot {
  @Id public UUID id;

  @Column(name = "empresa_id")
  public UUID empresaId;

  public int versao;
  public String estado;

  @Column(name = "nome_bot")
  public String nomeBot;

  @Column(name = "tom_voz")
  public String tomVoz;
  @Column(name = "usar_emojis")
  public boolean usarEmojis = true;
  public String instrucoes;

  @Column(name = "instrucoes_transferencia")
  public String instrucoesTransferencia;

  @Column(name = "autor_id")
  public UUID autorId;

  @Column(name = "publicado_em")
  public Instant publicadoEm;

  @Version public long revisao;

  @Column(name = "criado_em")
  public Instant criadoEm;

  @Column(name = "atualizado_em")
  public Instant atualizadoEm;

  protected ConfiguracaoBot() {}
}
