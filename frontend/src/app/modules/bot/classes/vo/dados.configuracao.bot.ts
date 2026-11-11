export class DadosConfiguracaoBot {
    nome!: string;
    modoGuiado = false;
    instrucoesLivres?: string;
    questionario = {pagamentos: "", entrega: "", horarios: "", perguntasFrequentes: "", chamarPessoa: ""};
    tomVoz!: string;
    usarEmojis!: boolean;
    instrucoes!: string;
    instrucoesTransferencia!: string;
    revisao = 0;
}
