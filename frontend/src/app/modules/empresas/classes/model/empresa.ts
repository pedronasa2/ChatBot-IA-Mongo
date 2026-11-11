import {ConfiguracaoBot} from "../../../bot/classes/model/configuracao.bot";
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

    bot!: ConfiguracaoBot;



    revisao: number = 0;

    criadoEm!: string;

    atualizadoEm!: string;
}
