import {Component, inject} from "@angular/core";
import {Routes} from "@angular/router";
import {SaidaEditoresService} from "./modules/core/security/saida.editores.service";

@Component({standalone: true, template: ""})
class PaginaInicialVazia {}

export const rotas: Routes = [
    {
        path: "",
        pathMatch: "full",
        component: PaginaInicialVazia,
        canDeactivate: [() => inject(SaidaEditoresService).autorizar()]
    },
    {path: "**", redirectTo: ""}
];
