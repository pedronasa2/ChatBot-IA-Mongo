export class Empresa {
    id!: string;

    nome!: string;

    descricao!: string;

    endereco!: string;

    contato!: string;

    site!: string;

    servicos!: string;

    fusoHorario!: string;

    chatwootAccountId!: number | null;

    chatwootInboxId!: number | null;

    botAtivo!: boolean;

    moduloCandidatosHabilitado!: boolean;

    moduloAgendaHabilitado!: boolean;

    revisao: number = 0;

    criadoEm!: string;

    atualizadoEm!: string;
}
