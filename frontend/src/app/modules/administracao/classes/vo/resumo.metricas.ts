import {MetricasEmpresa} from "./metricas.empresa";
import {VolumePorMinuto} from "./volume.por.minuto";
export class ResumoMetricas {
    mensagensUltimoMinuto!: number;

    totalMensagens!: number;

    mensagensUltimas24Horas!: number;

    mensagensRecebidas!: number;

    mensagensEnviadas!: number;

    errosUltimas24Horas!: number;

    totalEmpresas!: number;

    totalUsuariosAtivos!: number;

    ultimaMensagem!: string | null;

    atualizadoEm!: string;

    volumePorMinuto!: VolumePorMinuto[];

    porEmpresa!: MetricasEmpresa[];
}
