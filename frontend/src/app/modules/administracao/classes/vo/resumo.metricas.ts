import {MetricasEmpresa} from "./metricas.empresa";
import {PeriodoMensagens} from "./periodo.mensagens";
export class ResumoMetricas {
    mensagensUltimoMinuto!: number;
    totalMensagens!: number;
    mensagensUltimas24Horas!: number;
    totalEmpresas!: number;
    totalUsuariosAtivos!: number;
    ultimaMensagem!: string | null;
    atualizadoEm!: string;
    fusoHorario!: string;
    volume!: PeriodoMensagens[];
    porEmpresa!: MetricasEmpresa[];
}
