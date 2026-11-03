package br.com.painel.empresas.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.*;

import java.time.Instant;
import java.util.UUID;

@org.springframework.data.annotation.TypeAlias("br.com.painel.empresas.Empresa")
@Document(collection = "empresas")
public class Empresa {
    @Id
    public UUID id;

    public String nome;

    public String descricao;

    public String endereco;

    public String contato;

    public String site;

    public String servicos;

    @Field("fuso_horario")
    public String fusoHorario;

    @Field("bot_ativo")
    public boolean botAtivo;

    @Field("modulo_candidatos_habilitado")
    public boolean moduloCandidatosHabilitado;

    @Field("modulo_agenda_habilitado")
    public boolean moduloAgendaHabilitado;

    @Field("chatwoot_account_id")
    public Long chatwootAccountId;

    @Field("chatwoot_inbox_id")
    public Long chatwootInboxId;

    @Version
    public Long revisao = 0L;

    @Field("criado_em")
    public Instant criadoEm;

    @Field("atualizado_em")
    public Instant atualizadoEm;

    public Empresa() {}
}
