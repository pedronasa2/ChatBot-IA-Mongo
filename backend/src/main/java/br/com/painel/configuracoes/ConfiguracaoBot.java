package br.com.painel.configuracoes;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.*;

@Document(collection = "versoes_configuracoes_bot")
public class ConfiguracaoBot {
  @Id public UUID id;

  @Field("empresa_id")
  public UUID empresaId;

  public int versao;
  public String estado;

  @Field("nome_bot")
  public String nomeBot;

  @Field("tom_voz")
  public String tomVoz;

  @Field("usar_emojis")
  public boolean usarEmojis = true;

  public String instrucoes;

  @Field("instrucoes_transferencia")
  public String instrucoesTransferencia;

  @Field("autor_id")
  public UUID autorId;

  @Field("publicado_em")
  public Instant publicadoEm;

  @Version public Long revisao = 0L;

  @Field("criado_em")
  public Instant criadoEm;

  @Field("atualizado_em")
  public Instant atualizadoEm;

  public ConfiguracaoBot() {}
}
