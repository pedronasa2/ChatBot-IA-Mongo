import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {RascunhoBot} from "../classes/model/rascunho.bot";
import {DadosRascunhoBot} from "../classes/vo/dados.rascunho.bot";

@Injectable({providedIn: "root"})
export class ConfiguracaoBotService {
    constructor(private readonly restService: RestService) {}

    consultar(empresaId: string): Observable<RascunhoBot> {
        return this.restService.get(`/api/empresas/${empresaId}/rascunho`, RascunhoBot);
    }
    salvar(empresaId: string, dados: DadosRascunhoBot): Observable<RascunhoBot> {
        return this.restService.put(`/api/empresas/${empresaId}/rascunho`, dados, RascunhoBot);
    }
}
