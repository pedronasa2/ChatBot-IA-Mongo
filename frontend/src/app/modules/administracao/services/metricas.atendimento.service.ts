import {Injectable} from "@angular/core";
import {Observable, map} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {MetricasEmpresa} from "../classes/vo/metricas.empresa";
import {ResumoMetricas} from "../classes/vo/resumo.metricas";
import {VolumePorMinuto} from "../classes/vo/volume.por.minuto";

@Injectable({providedIn: "root"})
export class MetricasAtendimentoService {
    constructor(private readonly restService: RestService) {}

    consultar(): Observable<ResumoMetricas> {
        return this.restService.get("/api/superusuario/metricas", ResumoMetricas).pipe(
            map((resumo) => {
                resumo.volumePorMinuto = resumo.volumePorMinuto.map((volume) =>
                    Object.assign(new VolumePorMinuto(), volume)
                );
                resumo.porEmpresa = resumo.porEmpresa.map((empresa) => Object.assign(new MetricasEmpresa(), empresa));
                return resumo;
            })
        );
    }
}
