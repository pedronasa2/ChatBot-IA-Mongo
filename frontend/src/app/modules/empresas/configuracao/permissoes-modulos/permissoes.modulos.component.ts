import {Component, DestroyRef, effect, inject, input, output, signal, untracked} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormBuilder} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../../ui/ui.module";

import {Empresa} from "../../classes/model/empresa";
import {PermissoesModulosService} from "../../services/permissoes.modulos.service";
@Component({
    selector: "permissoes-editor",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./permissoes.modulos.component.html",
    styleUrl: "./permissoes.modulos.component.scss"
})
export class PermissoesModulosComponent {
    private fb = inject(FormBuilder);
    private api = inject(PermissoesModulosService);
    private destroy = inject(DestroyRef);
    readonly empresa = input.required<Empresa>();
    readonly superusuario = input(false);
    readonly pendente = output<boolean>();
    readonly ocupado = output<boolean>();
    readonly salvo = output<Empresa>();
    readonly falha = output<unknown>();
    readonly processando = signal(false);
    readonly modulosEmpresa = this.fb.nonNullable.group({
        moduloCandidatosHabilitado: [false],
        moduloAgendaHabilitado: [false],
        revisao: [0]
    });
    private contexto = "";
    constructor() {
        this.modulosEmpresa.valueChanges
            .pipe(takeUntilDestroyed(this.destroy))
            .subscribe(() => this.pendente.emit(this.modulosEmpresa.dirty));
        effect(() => {
            const atual = this.empresa();
            const id = this.empresa().id;
            untracked(() => {
                if (this.contexto !== id || (!this.modulosEmpresa.dirty && !this.processando())) {
                    this.modulosEmpresa.reset(atual);
                    this.contexto = id;
                }
            });
        });
    }
    descartar(): void {
        this.modulosEmpresa.reset(this.empresa());
        this.pendente.emit(false);
    }
    async salvar(): Promise<void> {
        if (this.processando()) return;
        if (this.modulosEmpresa.invalid) {
            this.modulosEmpresa.markAllAsTouched();
            return;
        }
        const id = this.empresa().id;
        const dados = this.modulosEmpresa.getRawValue();
        this.processando.set(true);
        this.ocupado.emit(true);
        this.modulosEmpresa.disable({emitEvent: false});
        try {
            const atual = await firstValueFrom(this.api.salvar(id, dados));
            if (id !== this.empresa().id) return;
            this.modulosEmpresa.reset(atual);
            this.pendente.emit(false);
            this.salvo.emit(atual);
        } catch (erro) {
            if (id === this.empresa().id) this.falha.emit(erro);
        } finally {
            this.modulosEmpresa.enable({emitEvent: false});
            this.processando.set(false);
            this.ocupado.emit(false);
        }
    }
}
