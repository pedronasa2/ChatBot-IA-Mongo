package br.com.painel.configuracoes.service;

import br.com.painel.auditoria.repository.AuditoriaRepository;
import br.com.painel.configuracoes.model.ConfiguracaoBot;
import br.com.painel.configuracoes.model.vo.DadosRascunhoBot;
import br.com.painel.configuracoes.model.vo.RascunhoResposta;
import br.com.painel.configuracoes.repository.ConfiguracaoBotRepository;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.service.security.AcessoEmpresa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class ConfiguracaoBotService {
    private final AcessoEmpresa acesso;
    private final ConfiguracaoBotRepository configuracoesBot;
    private final AuditoriaRepository auditoria;

    public ConfiguracaoBotService(
            AcessoEmpresa acesso,
            ConfiguracaoBotRepository configuracoesBot,
            AuditoriaRepository auditoria) {
        this.acesso = acesso;
        this.configuracoesBot = configuracoesBot;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public RascunhoResposta rascunho(UUID empresaId, String email) {
        acesso.exigirVinculo(empresaId, email);
        return RascunhoResposta.de(
                configuracoesBot
                        .findByEmpresaIdAndEstado(empresaId, "rascunho")
                        .orElseThrow(
                                () ->
                                        new NegocioException(
                                                ErroNegocio.NAO_ENCONTRADO,
                                                "Rascunho não encontrado")));
    }

    @Transactional
    public RascunhoResposta salvarRascunho(UUID empresaId, DadosRascunhoBot dados, String email) {
        UUID usuarioResponsavelId = acesso.exigirVinculo(empresaId, email);
        ConfiguracaoBot rascunho =
                configuracoesBot.findByEmpresaIdAndEstado(empresaId, "rascunho").orElseThrow();
        if (rascunho.revisao != dados.revisao()) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "Rascunho alterado em outra sessão. Recarregue antes de salvar.");
        }
        rascunho.nomeBot = dados.nomeBot().strip();
        rascunho.tomVoz = dados.tomVoz();
        rascunho.usarEmojis = dados.usarEmojis();
        rascunho.instrucoes = dados.instrucoes();
        rascunho.instrucoesTransferencia = dados.instrucoesTransferencia();
        rascunho.autorId = usuarioResponsavelId;
        rascunho.atualizadoEm = Instant.now();
        configuracoesBot.save(rascunho);
        auditoria.auditar(empresaId, usuarioResponsavelId, rascunho.empresaId, "rascunho_salvo");
        return RascunhoResposta.de(rascunho);
    }
}
