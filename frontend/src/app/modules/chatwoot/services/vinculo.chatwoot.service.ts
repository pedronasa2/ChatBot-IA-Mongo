import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {DadosVinculoChatwoot} from "../classes/vo/dados.vinculo.chatwoot";
import {VinculoChatwootResposta} from "../classes/vo/vinculo.chatwoot.resposta";

@Injectable({providedIn: "root"})
export class VinculoChatwootService {
    constructor(private readonly restService: RestService) {}

    consultar(empresaId: string): Observable<VinculoChatwootResposta> {
        return this.restService.get(`/api/superusuario/empresas/${empresaId}/chatwoot`, VinculoChatwootResposta);
    }
    salvar(empresaId: string, dados: DadosVinculoChatwoot): Observable<VinculoChatwootResposta> {
        return this.restService.put(`/api/superusuario/empresas/${empresaId}/chatwoot`, dados, VinculoChatwootResposta);
    }
}
