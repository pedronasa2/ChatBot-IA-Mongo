import {HttpErrorResponse} from "@angular/common/http";
import {Injectable, inject, signal} from "@angular/core";
import {CanActivateFn, Router} from "@angular/router";
import {firstValueFrom} from "rxjs";
import {Empresa} from "../../empresas/classes/model/empresa";
import {ModuloEmpresa} from "../../empresas/classes/model/modulo.empresa";
import {EmpresaService} from "../../empresas/services/empresa.service";

@Injectable({providedIn: "root"})
export class AcessoModulosService {
    private readonly _empresaVerificada = signal<Empresa | null>(null);
    readonly empresaVerificada = this._empresaVerificada.asReadonly();

    definirEmpresaVerificada(valor: Empresa | null): void {
        this._empresaVerificada.set(valor);
    }

    private readonly _sessaoEncerrada = signal<boolean>(false);
    readonly sessaoEncerrada = this._sessaoEncerrada.asReadonly();

    definirSessaoEncerrada(valor: boolean): void {
        this._sessaoEncerrada.set(valor);
    }

    private readonly _negacao = signal<string>("");
    readonly negacao = this._negacao.asReadonly();

    definirNegacao(valor: string): void {
        this._negacao.set(valor);
    }

    private readonly empresas = inject(EmpresaService);

    async verificar(empresaId: string, modulo: ModuloEmpresa): Promise<boolean> {
        this._negacao.set("");
        try {
            const empresa = await firstValueFrom(this.empresas.consultar(empresaId));
            this._empresaVerificada.set(empresa);
            if (modulo === "candidatos" ? empresa.moduloCandidatosHabilitado : empresa.moduloAgendaHabilitado)
                return true;
            this._negacao.set(
                modulo === "candidatos"
                    ? "Banco de candidatos desabilitado para esta empresa."
                    : "Agenda de atendimentos desabilitada para esta empresa."
            );
        } catch (erro) {
            if (erro instanceof HttpErrorResponse && erro.status === 401) this._sessaoEncerrada.set(true);
            if (!(erro instanceof HttpErrorResponse && erro.status === 401)) {
                this._negacao.set("Não foi possível acessar o módulo desta empresa.");
            }
        }
        return false;
    }
}

export const protegerModuloEmpresa: CanActivateFn = async (rota) => {
    const acesso = inject(AcessoModulosService);
    const router = inject(Router);
    const empresaId = rota.paramMap.get("empresaId") || "";
    return (await acesso.verificar(empresaId, rota.data["modulo"])) ? true : router.createUrlTree(["/"]);
};
