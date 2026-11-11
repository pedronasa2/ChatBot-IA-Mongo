package br.com.painel.empresas.service;

import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.service.security.AcessoEmpresa;
import br.com.painel.core.service.security.AcessoSuperusuario;
import br.com.painel.empresas.model.Empresa;
import br.com.painel.empresas.model.vo.DadosEmpresa;
import br.com.painel.empresas.model.vo.EmpresaResposta;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.superusuario.model.vo.DadosChatwoot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class EmpresaService {

    private final AcessoEmpresa acesso;

    private final EmpresaRepository empresas;

    private final AcessoSuperusuario superusuario;

    public EmpresaService(
            AcessoEmpresa acesso, EmpresaRepository empresas, AcessoSuperusuario superusuario) {
        this.acesso = acesso;
        this.empresas = empresas;
        this.superusuario = superusuario;
    }

    @Transactional(readOnly = true)
    public EmpresaResposta empresa(UUID empresaId, String email) {
        acesso.exigirVinculo(empresaId, email);
        return EmpresaResposta.de(empresas.findById(empresaId).orElseThrow());
    }

    @Transactional
    public EmpresaResposta salvarEmpresa(UUID empresaId, DadosEmpresa dados, String email) {
        UUID usuarioResponsavelId = acesso.exigirVinculo(empresaId, email);
        try {
            ZoneId.of(dados.fusoHorario());
        } catch (DateTimeException erro) {
            throw new NegocioException(ErroNegocio.DADOS_INVALIDOS, "Fuso horário inválido");
        }
        if (!dados.site().isBlank()) {
            try {
                var uri = java.net.URI.create(dados.site());
                if (!Set.of("https", "http").contains(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException erro) {
                throw new NegocioException(
                        ErroNegocio.DADOS_INVALIDOS, "Informe um site com http:// ou https://");
            }
        }
        Empresa empresa = empresas.findById(empresaId).orElseThrow();
        if (empresa.revisao != dados.revisao()) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "Dados alterados em outra sessão. Recarregue antes de salvar.");
        }
        empresa.nome = dados.nome().strip();
        empresa.descricao = dados.descricao();
        empresa.endereco = dados.endereco();
        empresa.contato = dados.contato();
        empresa.site = dados.site();
        empresa.servicos = dados.servicos();
        empresa.fusoHorario = dados.fusoHorario();
        empresa.atualizadoEm = Instant.now();
        empresas.save(empresa);
        return EmpresaResposta.de(empresa);
    }

    public EmpresaResposta salvarChatwoot(UUID id, DadosChatwoot dados, String email) {
        superusuario.exigir(() -> email);
        var e =
            empresas.findById(id).orElseThrow(
                () -> new NegocioException(
                            ErroNegocio.NAO_ENCONTRADO,
                            "Empresa não encontrada."));

        if (e.revisao != dados.revisao()) {
            throw new NegocioException(
                ErroNegocio.CONFLITO,
                "Dados alterados em outra sessão. Recarregue antes de salvar.");
        }
        e.chatwootAccountId = dados.chatwootAccountId();
        e.chatwootInboxId = dados.chatwootInboxId();
        e.atualizadoEm = Instant.now();
        try {
            return EmpresaResposta.de(empresas.save(e));
        } catch (org.springframework.dao.DuplicateKeyException erro) {
            throw new NegocioException(
                ErroNegocio.CONFLITO,
                "Esta conta e caixa do Chatwoot já estão vinculadas a outra empresa.");
        }
    }
}
