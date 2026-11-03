import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {AtivacaoConvite} from "../classes/vo/ativacao.convite";
import {ConviteCriado} from "../classes/vo/convite.criado";
import {NovaEmpresa} from "../classes/vo/nova.empresa";
import {SituacaoConvite} from "../classes/vo/situacao.convite";

@Injectable({providedIn: "root"})
export class ConviteService {
    constructor(private readonly restService: RestService) {}

    consultar(empresaId: string): Observable<SituacaoConvite> {
        return this.restService.get(`/api/superusuario/empresas/${empresaId}/convite`, SituacaoConvite);
    }
    criar(dados: NovaEmpresa): Observable<ConviteCriado> {
        return this.restService.post("/api/superusuario/empresas", dados, ConviteCriado);
    }
    reenviar(empresaId: string): Observable<ConviteCriado> {
        return this.restService.post(`/api/superusuario/empresas/${empresaId}/convite`, {}, ConviteCriado);
    }
    ativar(dados: AtivacaoConvite): Observable<void> {
        return this.restService.postSemResposta("/api/auth/ativar-convite", dados);
    }
}
