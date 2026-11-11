package br.com.painel.configuracoes.model;

public class ConfiguracaoBot {

    public boolean ativo;
    public boolean modoGuiado;
    public QuestionarioBot questionario = new QuestionarioBot();
    public String instrucoesLivres;
    public String instrucoesTransferenciaLivres;


    public String nome = "Assistente";

    public String tomVoz = "cordial";

    public boolean usarEmojis;

    public String instrucoes = "";

    public String instrucoesTransferencia = "";
}
