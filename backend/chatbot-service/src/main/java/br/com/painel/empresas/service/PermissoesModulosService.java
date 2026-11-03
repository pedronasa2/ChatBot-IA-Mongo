package br.com.painel.empresas.service;

import br.com.painel.auditoria.repository.AuditoriaRepository;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.service.security.AcessoSuperusuario;
import br.com.painel.empresas.model.vo.EmpresaResposta;
import br.com.painel.empresas.model.vo.PermissoesModulos;
import br.com.painel.empresas.repository.ControleEmpresaRepository;
import br.com.painel.empresas.repository.EmpresaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PermissoesModulosService {
    private final ControleEmpresaRepository controle;
    private final EmpresaRepository empresas;
    private final AuditoriaRepository auditoria;
    private final AcessoSuperusuario acesso;

    public PermissoesModulosService(
            ControleEmpresaRepository controle,
            EmpresaRepository empresas,
            AuditoriaRepository auditoria,
            AcessoSuperusuario acesso) {
        this.controle = controle;
        this.empresas = empresas;
        this.auditoria = auditoria;
        this.acesso = acesso;
    }

    @Transactional
    public EmpresaResposta atualizar(UUID empresaId, PermissoesModulos dados, String email) {
        UUID responsavel = acesso.exigir(() -> email);
        boolean atualizado = controle.atualizarPermissoes(empresaId, dados);
        if (!atualizado) {
            if (!empresas.existsById(empresaId)) {
                throw new NegocioException(ErroNegocio.NAO_ENCONTRADO, "Empresa não encontrada.");
            }
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "Empresa alterada em outra sessão. Recarregue antes de salvar as permissões.");
        }
        auditoria.auditar(empresaId, responsavel, null, "permissoes_modulos_atualizadas");
        return EmpresaResposta.de(empresas.findById(empresaId).orElseThrow());
    }
}
