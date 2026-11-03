package br.com.painel.empresas.model.vo;

import br.com.painel.empresas.model.Empresa;

import java.time.Instant;
import java.util.UUID;

public record EmpresaResposta(
        UUID id,
        String nome,
        String descricao,
        String endereco,
        String contato,
        String site,
        String servicos,
        String fusoHorario,
        boolean botAtivo,
        boolean moduloCandidatosHabilitado,
        boolean moduloAgendaHabilitado,
        Long chatwootAccountId,
        Long chatwootInboxId,
        Long revisao,
        Instant criadoEm,
        Instant atualizadoEm) {
    public static EmpresaResposta de(Empresa e) {
        return new EmpresaResposta(
                e.id,
                e.nome,
                e.descricao,
                e.endereco,
                e.contato,
                e.site,
                e.servicos,
                e.fusoHorario,
                e.botAtivo,
                e.moduloCandidatosHabilitado,
                e.moduloAgendaHabilitado,
                e.chatwootAccountId,
                e.chatwootInboxId,
                e.revisao,
                e.criadoEm,
                e.atualizadoEm);
    }
}
