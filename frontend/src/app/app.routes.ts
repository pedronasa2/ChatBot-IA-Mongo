import {Component, inject} from "@angular/core";
import {CanDeactivateFn, Routes} from "@angular/router";
import {protegerModuloEmpresa} from "./modules/core/security/acesso.modulos.service";
import {SaidaEditoresService} from "./modules/core/security/saida.editores.service";

@Component({standalone: true, template: ""})
class PaginaInicialVazia {}

export const protegerEdicao: CanDeactivateFn<{podeSair(): boolean}> = (componente) => componente.podeSair();

export const rotas: Routes = [
    {
        path: "",
        pathMatch: "full",
        component: PaginaInicialVazia,
        canDeactivate: [() => inject(SaidaEditoresService).autorizar()]
    },
    {
        path: "empresas/:empresaId/candidatos",
        loadComponent: () =>
            import("./modules/candidatos/cadastro/candidato-crud/candidato.crud.component").then(
                (modulo) => modulo.CandidatoCrudComponent
            ),
        canDeactivate: [protegerEdicao],
        runGuardsAndResolvers: "paramsChange",
        canActivate: [protegerModuloEmpresa],
        data: {modulo: "candidatos"}
    },
    {
        path: "empresas/:empresaId/agenda",
        loadComponent: () =>
            import("./modules/agenda/cadastro/agendamento-crud/agendamento.crud.component").then(
                (modulo) => modulo.AgendamentoCrudComponent
            ),
        canDeactivate: [protegerEdicao],
        runGuardsAndResolvers: "paramsChange",
        canActivate: [protegerModuloEmpresa],
        data: {modulo: "agenda"}
    },
    {path: "**", redirectTo: ""}
];
