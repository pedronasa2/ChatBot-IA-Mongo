import {ConfiguracaoBot} from "../../../bot/classes/model/configuracao.bot";
export class Empresa {
    id!: string;

    nome!: string;
    corPrimaria: string = "#0d3b4c";
    corSecundaria: string = "#525e7f";
    fotoEmpresa: string = "";

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
