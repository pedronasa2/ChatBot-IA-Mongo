package br.com.painel.configuracoes.model;
import jakarta.validation.constraints.Size;
public class QuestionarioBot {
    @Size(max=1500) public String pagamentos = "";
    @Size(max=1500) public String entrega = "";
    @Size(max=1500) public String horarios = "";
    @Size(max=3000) public String perguntasFrequentes = "";
    @Size(max=1500) public String chamarPessoa = "";
}
