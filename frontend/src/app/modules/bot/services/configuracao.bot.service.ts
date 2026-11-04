import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {Empresa} from "../../empresas/classes/model/empresa";
import {DadosConfiguracaoBot} from "../classes/vo/dados.configuracao.bot";
@Injectable({providedIn: "root"})
export class ConfiguracaoBotService {
    constructor(private readonly restService: RestService) {}
    salvar(empresaId: string, dados: DadosConfiguracaoBot): Observable<Empresa> {
        return this.restService.put(`/api/empresas/${empresaId}/bot`, dados, Empresa);
    }
}
