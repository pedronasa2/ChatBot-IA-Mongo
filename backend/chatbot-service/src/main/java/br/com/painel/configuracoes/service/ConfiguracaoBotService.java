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
        e.bot.instrucoes = dados.instrucoes();
        e.bot.instrucoesTransferencia = dados.instrucoesTransferencia();
        e.atualizadoEm = Instant.now();
        return EmpresaResposta.de(empresas.save(e));
    }
}
