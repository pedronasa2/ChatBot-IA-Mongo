export class Candidato {
    id!: string;

    empresaId!: string;

    nomeCompleto!: string;

    email!: string;

    telefone!: string;

    cargoPretendido!: string;

    urlCurriculo!: string;

    resumoProfissional!: string;

    observacoes!: string;

    situacao!: string;

    arquivado!: boolean;

    revisao: number = 0;

    recebidoEm!: string;

    atualizadoEm!: string;
}
