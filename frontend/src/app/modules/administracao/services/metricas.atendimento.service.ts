import {Injectable} from "@angular/core";
import {HttpParams} from "@angular/common/http";
import {Observable, map} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {MetricasEmpresa} from "../classes/vo/metricas.empresa";
import {ResumoMetricas} from "../classes/vo/resumo.metricas";
import {PeriodoMensagens} from "../classes/vo/periodo.mensagens";
@Injectable({providedIn: "root"})
export class MetricasAtendimentoService {
    constructor(private readonly restService: RestService) {}
    consultar(filtro?: {empresaId: string; intervalo: string; de: string; ate: string}): Observable<ResumoMetricas> {
        let params = new HttpParams();
        if (filtro) {
            params = params.set("intervalo", filtro.intervalo);
            if (filtro.empresaId) params = params.set("empresaId", filtro.empresaId);
            if (filtro.de) params = params.set("de", new Date(filtro.de).toISOString());
            if (filtro.ate) params = params.set("ate", new Date(filtro.ate).toISOString());
        }
        return this.restService.get("/api/superusuario/metricas", ResumoMetricas, params).pipe(map(resumo => {
            resumo.volume = resumo.volume.map(v => Object.assign(new PeriodoMensagens(), v));
            resumo.porEmpresa = resumo.porEmpresa.map(e => Object.assign(new MetricasEmpresa(), e));
            return resumo;
        }));
    }
}
