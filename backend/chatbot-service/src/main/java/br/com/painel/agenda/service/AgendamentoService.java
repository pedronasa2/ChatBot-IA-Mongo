package br.com.painel.agenda.service;

import br.com.painel.agenda.model.vo.Agendamento;
import br.com.painel.agenda.model.vo.DadosAgendamento;
import br.com.painel.agenda.repository.AgendamentoRepository;
import br.com.painel.core.model.PaginaRegistros;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.model.security.Credenciais;
import br.com.painel.core.model.security.Modulo;
import br.com.painel.core.service.security.AcessoModulos;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AgendamentoService {
    private final AgendamentoRepository registros;
    private final AcessoModulos acesso;

    public AgendamentoService(AgendamentoRepository registros, AcessoModulos acesso) {
        this.registros = registros;
        this.acesso = acesso;
    }

    public PaginaRegistros<Agendamento> listar(
            UUID empresaId,
            Credenciais credenciais,
            String busca,
            boolean incluirArquivados,
            Instant de,
            Instant ate,
            int pagina,
            int tamanho) {
        acesso.exigir(empresaId, Modulo.AGENDA, credenciais);
        if (de != null && ate != null && !ate.isAfter(de)) {
            throw new NegocioException(ErroNegocio.DADOS_INVALIDOS, "Período de busca inválido.");
        }
        return registros.listar(empresaId, busca, incluirArquivados, de, ate, pagina, tamanho);
    }

    public Agendamento consultar(UUID empresaId, UUID id, Credenciais credenciais) {
        acesso.exigir(empresaId, Modulo.AGENDA, credenciais);
        return registros.consultar(empresaId, id);
    }

    private void validar(DadosAgendamento dados) {
        if (!dados.fim().isAfter(dados.inicio())) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS,
                    "O fim do atendimento deve ser posterior ao início.");
        }
    }

    public Agendamento criar(UUID empresaId, DadosAgendamento dados, Credenciais credenciais) {
        acesso.exigir(empresaId, Modulo.AGENDA, credenciais);
        validar(dados);
        return registros.criar(empresaId, dados);
    }

    public Agendamento atualizar(
            UUID empresaId, UUID id, DadosAgendamento dados, Credenciais credenciais) {
        acesso.exigir(empresaId, Modulo.AGENDA, credenciais);
        validar(dados);
        return registros.atualizar(empresaId, id, dados);
    }

    public void arquivar(UUID empresaId, UUID id, long revisao, Credenciais credenciais) {
        acesso.exigir(empresaId, Modulo.AGENDA, credenciais);
        registros.arquivar(empresaId, id, revisao);
    }
}
