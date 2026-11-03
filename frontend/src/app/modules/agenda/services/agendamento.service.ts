import {HttpParams} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {PaginaRegistros} from "../../core/classes/vo/pagina.registros";
import {RestService} from "../../core/http/rest.service";
import {Agendamento} from "../classes/model/agendamento";
import {AgendamentoFilterVO} from "../classes/vo/agendamento.filter.vo";
import {DadosAgendamento} from "../classes/vo/dados.agendamento";

@Injectable({providedIn: "root"})
export class AgendamentoService {
    constructor(private readonly restService: RestService) {}

    private endereco(empresaId: string): string {
        return `/api/empresas/${empresaId}/agenda`;
    }

    listar(empresaId: string, filtro: AgendamentoFilterVO, pagina: number): Observable<PaginaRegistros<Agendamento>> {
        let parametros = new HttpParams()
            .set("busca", filtro.busca)
            .set("incluirArquivados", filtro.incluirArquivados)
            .set("pagina", pagina)
            .set("tamanho", 20);
        if (filtro.de) parametros = parametros.set("de", new Date(filtro.de).toISOString());
        if (filtro.ate) parametros = parametros.set("ate", new Date(filtro.ate).toISOString());
        return this.restService.pagina(this.endereco(empresaId), Agendamento, parametros);
    }

    consultar(empresaId: string, id: string): Observable<Agendamento> {
        return this.restService.get(`${this.endereco(empresaId)}/${id}`, Agendamento);
    }

    salvar(empresaId: string, id: string | undefined, dados: DadosAgendamento): Observable<Agendamento> {
        return id
            ? this.restService.put(`${this.endereco(empresaId)}/${id}`, dados, Agendamento)
            : this.restService.post(this.endereco(empresaId), dados, Agendamento);
    }

    arquivar(empresaId: string, registro: Agendamento): Observable<void> {
        return this.restService.delete(
            `${this.endereco(empresaId)}/${registro.id}`,
            new HttpParams().set("revisao", registro.revisao)
        );
    }
}
