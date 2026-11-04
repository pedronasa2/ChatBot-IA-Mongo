package br.com.painel.superusuario.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "configuracoes_sistema")
public class ConfiguracaoSistema {

    @Id public String id = "principal";

    public String promptGlobal = "";

    public boolean complementarConfiguracoesEmpresa = true;

    @Version public Long revisao = 0L;

    public Instant atualizadoEm;
}
