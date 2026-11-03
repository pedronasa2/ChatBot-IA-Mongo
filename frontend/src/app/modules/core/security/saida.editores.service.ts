import {Injectable} from "@angular/core";
@Injectable({providedIn: "root"})
export class SaidaEditoresService {
    private verificar: () => boolean = () => true;
    registrar(verificar: () => boolean): void {
        this.verificar = verificar;
    }
    limpar(): void {
        this.verificar = () => true;
    }
    autorizar(): boolean {
        return this.verificar();
    }
}
