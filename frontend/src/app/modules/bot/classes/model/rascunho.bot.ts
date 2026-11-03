export class RascunhoBot {
    id!: string;

    empresaId!: string;

    versao!: number;

    estado!: string;

    nomeBot!: string;

    tomVoz!: string;

    usarEmojis!: boolean;

    instrucoes!: string;

    instrucoesTransferencia!: string;

    autorId!: string;

    publicadoEm!: string | null;

    revisao: number = 0;

    criadoEm!: string;

    atualizadoEm!: string;
}
