import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {Empresa} from "../classes/model/empresa";
import {DadosEmpresa} from "../classes/vo/dados.empresa";
import {EmpresaAcessivel} from "../classes/vo/empresa.acessivel";

@Injectable({providedIn: "root"})
export class EmpresaService {
    constructor(private readonly restService: RestService) {}

    listar(): Observable<EmpresaAcessivel[]> {
        return this.restService.list("/api/empresas", EmpresaAcessivel);
    }
    consultar(empresaId: string): Observable<Empresa> {
        return this.restService.get(`/api/empresas/${empresaId}`, Empresa);
    }
    salvarChatwoot(empresaId: string, dados: {chatwootAccountId: number; chatwootInboxId: number; revisao: number}): Observable<Empresa> {
        return this.restService.put(`/api/superusuario/empresas/${empresaId}/chatwoot`, dados, Empresa);
    }
    salvar(empresaId: string, dados: DadosEmpresa): Observable<Empresa> {
        return this.restService.put(`/api/empresas/${empresaId}`, dados, Empresa);
    }
}
