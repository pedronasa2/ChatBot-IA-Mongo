import {Injectable, signal} from "@angular/core";
@Injectable({providedIn: "root"})
export class EstadoEdicaoModulo {
    private readonly estado = signal(false);
    readonly pendente = this.estado.asReadonly();
    definir(pendente: boolean): void {
        this.estado.set(pendente);
    }
}
