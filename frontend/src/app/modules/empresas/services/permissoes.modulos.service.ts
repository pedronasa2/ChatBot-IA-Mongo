import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {Empresa} from "../classes/model/empresa";
import {PermissoesModulos} from "../classes/vo/permissoes.modulos";

@Injectable({providedIn: "root"})
export class PermissoesModulosService {
    constructor(private readonly restService: RestService) {}

    salvar(empresaId: string, dados: PermissoesModulos): Observable<Empresa> {
        return this.restService.put(`/api/superusuario/empresas/${empresaId}/modulos`, dados, Empresa);
    }
}
