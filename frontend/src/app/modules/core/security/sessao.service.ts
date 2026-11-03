import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Injectable, inject} from "@angular/core";
import {firstValueFrom} from "rxjs";
import {Identidade} from "../classes/vo/identidade";
import {TokenCsrf} from "../classes/vo/token.csrf";

@Injectable({providedIn: "root"})
export class SessaoService {
    private readonly http = inject(HttpClient);
    private csrf: TokenCsrf | null = null;
    async renovarCsrf(): Promise<void> {
        this.csrf = Object.assign(new TokenCsrf(), await firstValueFrom(this.http.get<TokenCsrf>("/api/auth/csrf")));
    }
    cabecalhos(): HttpHeaders {
        if (!this.csrf) throw new Error("Sessão indisponível");
        return new HttpHeaders({[this.csrf.headerName]: this.csrf.token});
    }
    async opcoes(): Promise<{headers: HttpHeaders}> {
        await this.renovarCsrf();
        return {headers: this.cabecalhos()};
    }
    async identidade(): Promise<Identidade> {
        return Object.assign(new Identidade(), await firstValueFrom(this.http.get<Identidade>("/api/auth/me")));
    }
    async entrar(dados: {email: string; senha: string}): Promise<void> {
        await this.renovarCsrf();
        await firstValueFrom(
            this.http.post("/api/auth/login", new URLSearchParams(dados).toString(), {
                headers: this.cabecalhos().set("Content-Type", "application/x-www-form-urlencoded")
            })
        );
        await this.renovarCsrf();
    }
    async sair(): Promise<void> {
        await firstValueFrom(this.http.post("/api/auth/logout", {}, await this.opcoes()));
        await this.renovarCsrf();
    }
}
