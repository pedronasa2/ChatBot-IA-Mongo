package br.com.painel.empresas;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "empresas")
public class Empresa {
  @Id public UUID id;
  public String nome;
  public String descricao;
  public String endereco;
  public String contato;
  public String site;
  public String servicos;

  @Column(name = "fuso_horario")
  public String fusoHorario;

  @Column(name = "bot_ativo")
  public boolean botAtivo;

  @Version public long revisao;

  @Column(name = "criado_em")
  public Instant criadoEm;

  @Column(name = "atualizado_em")
  public Instant atualizadoEm;

  protected Empresa() {}
}
