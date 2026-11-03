import {HttpParams} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {PaginaRegistros} from "../../core/classes/vo/pagina.registros";
import {RestService} from "../../core/http/rest.service";
import {Candidato} from "../classes/model/candidato";
import {CandidatoFilterVO} from "../classes/vo/candidato.filter.vo";
import {DadosCandidato} from "../classes/vo/dados.candidato";

@Injectable({providedIn: "root"})
export class CandidatoService {
    constructor(private readonly restService: RestService) {}

    private endereco(empresaId: string): string {
        return `/api/empresas/${empresaId}/candidatos`;
    }

    listar(empresaId: string, filtro: CandidatoFilterVO, pagina: number): Observable<PaginaRegistros<Candidato>> {
        let parametros = new HttpParams()
            .set("busca", filtro.busca)
            .set("incluirArquivados", filtro.incluirArquivados)
            .set("pagina", pagina)
            .set("tamanho", 20);
        return this.restService.pagina(this.endereco(empresaId), Candidato, parametros);
    }

    consultar(empresaId: string, id: string): Observable<Candidato> {
        return this.restService.get(`${this.endereco(empresaId)}/${id}`, Candidato);
    }

    salvar(empresaId: string, id: string | undefined, dados: DadosCandidato): Observable<Candidato> {
        return id
            ? this.restService.put(`${this.endereco(empresaId)}/${id}`, dados, Candidato)
            : this.restService.post(this.endereco(empresaId), dados, Candidato);
    }

    arquivar(empresaId: string, registro: Candidato): Observable<void> {
        return this.restService.delete(
            `${this.endereco(empresaId)}/${registro.id}`,
            new HttpParams().set("revisao", registro.revisao)
        );
    }

    enderecoCurriculo(empresaId: string, id: string): string {
        return `${this.endereco(empresaId)}/${id}/curriculo`;
    }
}
