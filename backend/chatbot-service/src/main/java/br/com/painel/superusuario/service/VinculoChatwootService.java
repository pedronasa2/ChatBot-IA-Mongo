package br.com.painel.superusuario.service;

import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.service.security.AcessoSuperusuario;
import br.com.painel.empresas.model.Empresa;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.empresas.repository.VinculoChatwootRepository;
import br.com.painel.superusuario.model.vo.DadosVinculoChatwoot;
import br.com.painel.superusuario.model.vo.VinculoChatwoot;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VinculoChatwootService {
    private final EmpresaRepository empresas;
    private final VinculoChatwootRepository vinculos;
    private final AcessoSuperusuario acesso;

    public VinculoChatwootService(
            EmpresaRepository empresas,
            VinculoChatwootRepository vinculos,
            AcessoSuperusuario acesso) {
        this.empresas = empresas;
        this.vinculos = vinculos;
        this.acesso = acesso;
    }

    private Empresa verificar(UUID id, String email) {
        acesso.exigir(() -> email);
        return empresas.findById(id)
                .orElseThrow(
                        () ->
                                new NegocioException(
                                        ErroNegocio.NAO_ENCONTRADO, "Empresa não encontrada."));
    }

    public VinculoChatwoot consultar(UUID id, String email) {
        var e = verificar(id, email);
        return new VinculoChatwoot(id, e.chatwootAccountId, e.chatwootInboxId);
    }

    public VinculoChatwoot salvar(UUID id, DadosVinculoChatwoot dados, String email) {
        verificar(id, email);
        try {
            vinculos.salvar(id, dados, acesso.exigir(() -> email));
        } catch (DuplicateKeyException e) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "Esta conta e caixa do Chatwoot já estão vinculadas a outra empresa.");
        }
        return new VinculoChatwoot(id, dados.accountId(), dados.inboxId());
    }
}
