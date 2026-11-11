package br.com.painel.empresas.model;

import br.com.painel.configuracoes.model.ConfiguracaoBot;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "empresas")
public class Empresa {

    @Id public UUID id;

    public String nome;

    public String corPrimaria = "#0d3b4c";
    public String corSecundaria = "#525e7f";
    public String fotoEmpresa = "";

    public String descricao = "";

    public String endereco = "";

    public String contato = "";

    public String site = "";

    public String servicos = "";

    public String fusoHorario = "America/Sao_Paulo";

    public ConfiguracaoBot bot = new ConfiguracaoBot();

    public Long chatwootAccountId;

    public Long chatwootInboxId;

    @Version public Long revisao = 0L;

    public Instant criadoEm;

    public Instant atualizadoEm;
}
