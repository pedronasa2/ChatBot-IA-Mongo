export class ConfiguracaoBot {
    ativo!: boolean;
    nome!: string;
    modoGuiado = false;
    instrucoesLivres?: string;
    instrucoesTransferenciaLivres?: string;
    questionario = {pagamentos: "", entrega: "", horarios: "", perguntasFrequentes: "", chamarPessoa: ""};
    tomVoz!: string;
    usarEmojis!: boolean;
    instrucoes!: string;
    instrucoesTransferencia!: string;
}
