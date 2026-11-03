package br.com.painel.candidatos.service;

import br.com.painel.candidatos.model.vo.Candidato;
import br.com.painel.candidatos.model.vo.DadosCandidato;
import br.com.painel.candidatos.repository.CandidatoRepository;
import br.com.painel.core.model.PaginaRegistros;
import br.com.painel.core.model.security.Credenciais;
import br.com.painel.core.model.security.Modulo;
import br.com.painel.core.service.security.AcessoModulos;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CandidatoService {
    private final CandidatoRepository registros;
    private final AcessoModulos acesso;

    public CandidatoService(CandidatoRepository registros, AcessoModulos acesso) {
        this.registros = registros;
        this.acesso = acesso;
    }

    public PaginaRegistros<Candidato> listar(
            UUID empresaId,
            Credenciais credenciais,
            String busca,
            boolean incluirArquivados,
            int pagina,
            int tamanho) {
        acesso.exigir(empresaId, Modulo.CANDIDATOS, credenciais);
        return registros.listar(empresaId, busca, incluirArquivados, pagina, tamanho);
    }

    public Candidato consultar(UUID empresaId, UUID id, Credenciais credenciais) {
        acesso.exigir(empresaId, Modulo.CANDIDATOS, credenciais);
        return registros.consultar(empresaId, id);
    }

    private void validar(DadosCandidato dados) {
        CandidatoValidator.urlCurriculo(dados.urlCurriculo());
    }

    public Candidato criar(UUID empresaId, DadosCandidato dados, Credenciais credenciais) {
        acesso.exigir(empresaId, Modulo.CANDIDATOS, credenciais);
        validar(dados);
        return registros.criar(empresaId, dados);
    }

    public Candidato atualizar(
            UUID empresaId, UUID id, DadosCandidato dados, Credenciais credenciais) {
        acesso.exigir(empresaId, Modulo.CANDIDATOS, credenciais);
        validar(dados);
        return registros.atualizar(empresaId, id, dados);
    }

    public void arquivar(UUID empresaId, UUID id, long revisao, Credenciais credenciais) {
        acesso.exigir(empresaId, Modulo.CANDIDATOS, credenciais);
        registros.arquivar(empresaId, id, revisao);
    }
}
