package br.com.painel.empresas.model.vo;

import br.com.painel.configuracoes.model.ConfiguracaoBot;
import br.com.painel.empresas.model.Empresa;

import java.time.Instant;
import java.util.UUID;

public record EmpresaResposta(
        UUID id,
        String nome,
        String corPrimaria,
        String corSecundaria,
        String fotoEmpresa,
        String descricao,
        String endereco,
        String contato,
        String site,
        String servicos,
        String fusoHorario,
        ConfiguracaoBot bot,
        Long chatwootAccountId,
        Long chatwootInboxId,
        Long revisao,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static EmpresaResposta de(Empresa e) {
        return new EmpresaResposta(
                e.id,
                e.nome,
                e.corPrimaria,
                e.corSecundaria,
                e.fotoEmpresa,
                e.descricao,
                e.endereco,
                e.contato,
                e.site,
                e.servicos,
                e.fusoHorario,
                e.bot,
                e.chatwootAccountId,
                e.chatwootInboxId,
                e.revisao,
                e.criadoEm,
                e.atualizadoEm);
    }
}
