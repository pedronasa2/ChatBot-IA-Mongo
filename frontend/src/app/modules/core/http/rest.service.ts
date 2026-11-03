import {HttpClient, HttpHeaders, HttpParams} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable, defer, from, map, mergeMap} from "rxjs";
import {PaginaRegistros} from "../classes/vo/pagina.registros";
import {SessaoService} from "../security/sessao.service";

type ClasseModelo<T extends object> = new () => T;

@Injectable({providedIn: "root"})
export class RestService {
    constructor(
        private readonly http: HttpClient,
        private readonly sessaoService: SessaoService
    ) {}

    get<T extends object>(url: string, modelo: ClasseModelo<T>, params?: HttpParams): Observable<T> {
        return this.http.get<T>(url, {params}).pipe(map((dados) => Object.assign(new modelo(), dados)));
    }

    list<T extends object>(url: string, modelo: ClasseModelo<T>): Observable<T[]> {
        return this.http.get<T[]>(url).pipe(map((dados) => dados.map((item) => Object.assign(new modelo(), item))));
    }

    pagina<T extends object>(url: string, modelo: ClasseModelo<T>, params: HttpParams): Observable<PaginaRegistros<T>> {
        return this.http.get<PaginaRegistros<T>>(url, {params}).pipe(
            map((dados) =>
                Object.assign(new PaginaRegistros<T>(), dados, {
                    registros: dados.registros.map((item) => Object.assign(new modelo(), item))
                })
            )
        );
    }

    post<T extends object>(url: string, dados: object, modelo: ClasseModelo<T>): Observable<T> {
        return this.comCsrf((opcoes) => this.http.post<T>(url, dados, opcoes)).pipe(
            map((resposta) => Object.assign(new modelo(), resposta))
        );
    }

    put<T extends object>(url: string, dados: object, modelo: ClasseModelo<T>): Observable<T> {
        return this.comCsrf((opcoes) => this.http.put<T>(url, dados, opcoes)).pipe(
            map((resposta) => Object.assign(new modelo(), resposta))
        );
    }

    postSemResposta(url: string, dados: object): Observable<void> {
        return this.comCsrf((opcoes) => this.http.post<void>(url, dados, opcoes));
    }

    delete(url: string, params: HttpParams): Observable<void> {
        return this.comCsrf((opcoes) => this.http.delete<void>(url, {...opcoes, params}));
    }

    private comCsrf<T>(operacao: (opcoes: {headers: HttpHeaders}) => Observable<T>): Observable<T> {
        return defer(() => from(this.sessaoService.opcoes())).pipe(mergeMap(operacao));
    }
}
