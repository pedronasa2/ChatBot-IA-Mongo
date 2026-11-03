package br.com.painel.core.service.security;

import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.model.security.Credenciais;
import br.com.painel.core.model.security.Modulo;
import br.com.painel.empresas.repository.ControleEmpresaRepository;
import br.com.painel.empresas.repository.EmpresaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
public class AcessoModulos {
    private final EmpresaRepository registros;
    private final ControleEmpresaRepository controle;
    private final AcessoEmpresa empresas;
    private final String tokenEsperado;

    public AcessoModulos(
            EmpresaRepository registros,
            ControleEmpresaRepository controle,
            AcessoEmpresa empresas,
            @Value("${painel.integracao.token-modulos:}") String tokenEsperado) {
        this.registros = registros;
        this.controle = controle;
        this.empresas = empresas;
        this.tokenEsperado = tokenEsperado;
    }

    public void exigir(UUID empresaId, Modulo modulo, Credenciais credenciais) {
        if (credenciais.emailUsuario() != null) {
            empresas.exigirVinculo(empresaId, credenciais.emailUsuario());
        } else {
            if (tokenEsperado.isBlank()) {
                throw new NegocioException(
                        ErroNegocio.INTEGRACAO_INDISPONIVEL,
                        "Integração dos módulos não configurada.");
            }
            String token =
                    credenciais.tokenIntegracao() == null ? "" : credenciais.tokenIntegracao();
            if (!MessageDigest.isEqual(
                    tokenEsperado.getBytes(StandardCharsets.UTF_8),
                    token.getBytes(StandardCharsets.UTF_8))) {
                throw new NegocioException(
                        ErroNegocio.NAO_AUTORIZADO, "Credencial da integração inválida.");
            }
        }
        var empresa = registros.findById(empresaId).orElse(null);
        if (empresa == null) {
            throw new NegocioException(ErroNegocio.NAO_ENCONTRADO, "Empresa não encontrada.");
        }
        boolean habilitado =
                modulo == Modulo.CANDIDATOS
                        ? empresa.moduloCandidatosHabilitado
                        : empresa.moduloAgendaHabilitado;
        if (!habilitado) {
            throw new NegocioException(
                    ErroNegocio.ACESSO_NEGADO, "Módulo desabilitado para esta empresa.");
        }
        controle.registrarOperacaoModulo(empresaId, modulo);
    }
}
