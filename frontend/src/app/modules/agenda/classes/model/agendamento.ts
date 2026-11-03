export class Agendamento {
    id!: string;

    empresaId!: string;

    nomePessoa!: string;

    email!: string;

    telefone!: string;

    assunto!: string;

    responsavel!: string;

    inicio!: string;

    fim!: string;

    situacao!: string;

    observacoes!: string;

    arquivado!: boolean;

    revisao: number = 0;

    criadoEm!: string;

    atualizadoEm!: string;
}
