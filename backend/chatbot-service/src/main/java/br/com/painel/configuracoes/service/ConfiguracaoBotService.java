package br.com.painel.configuracoes.service;

import br.com.painel.configuracoes.model.vo.DadosConfiguracaoBot;
import br.com.painel.core.model.exception.*;
import br.com.painel.core.service.security.AcessoEmpresa;
import br.com.painel.empresas.model.vo.EmpresaResposta;
import br.com.painel.empresas.repository.EmpresaRepository;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConfiguracaoBotService {

    private final AcessoEmpresa acesso;

    private final EmpresaRepository empresas;

    public ConfiguracaoBotService(AcessoEmpresa acesso, EmpresaRepository empresas) {
        this.acesso = acesso;
        this.empresas = empresas;
    }

    static String comporInstrucoes(String livres, boolean guiado, br.com.painel.configuracoes.model.QuestionarioBot q) {
        if (!guiado) return livres;
        StringBuilder texto = new StringBuilder("Use os dados da empresa já cadastrados. Não invente preços, condições ou informações ausentes. Se não souber, explique e ofereça atendimento humano.\n");
        String[][] campos = {{"Pagamento", q.pagamentos}, {"Entrega e retirada", q.entrega},
            {"Horários", q.horarios}, {"Perguntas frequentes", q.perguntasFrequentes}, {"Quando chamar uma pessoa", q.chamarPessoa}};
        for (var campo : campos) if (campo[1] != null && !campo[1].isBlank())
            texto.append(campo[0]).append(": ").append(campo[1].strip()).append("\n");
        if (!livres.isBlank()) texto.append("Orientações adicionais: ").append(livres);
        return texto.toString();
    }

    public EmpresaResposta salvar(UUID empresaId, DadosConfiguracaoBot dados, String email) {
        acesso.exigirVinculo(empresaId, email);
        var e = empresas.findById(empresaId).orElseThrow();
        if (e.revisao != dados.revisao()) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "Dados alterados em outra sessão. Recarregue antes de salvar.");
        }
        e.bot.nome = dados.nome().strip();
        e.bot.tomVoz = dados.tomVoz();
        e.bot.usarEmojis = dados.usarEmojis();
        e.bot.modoGuiado = Boolean.TRUE.equals(dados.modoGuiado());
        e.bot.questionario = dados.questionario() == null ? new br.com.painel.configuracoes.model.QuestionarioBot() : dados.questionario();
        e.bot.instrucoesLivres = dados.instrucoes();
        e.bot.instrucoes = comporInstrucoes(dados.instrucoes(), Boolean.TRUE.equals(dados.modoGuiado()), e.bot.questionario);
        e.bot.instrucoesTransferenciaLivres = dados.instrucoesTransferencia();
        e.bot.instrucoesTransferencia = Boolean.TRUE.equals(dados.modoGuiado())
                ? java.util.Objects.toString(e.bot.questionario.chamarPessoa, "") : dados.instrucoesTransferencia();
        e.atualizadoEm = Instant.now();
        return EmpresaResposta.de(empresas.save(e));
    }
}
