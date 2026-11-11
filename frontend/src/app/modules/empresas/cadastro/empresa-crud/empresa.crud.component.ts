import {Component, DestroyRef, effect, inject, input, output, signal, untracked} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormBuilder, Validators} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../../ui/ui.module";

import {Empresa} from "../../classes/model/empresa";
import {EmpresaService} from "../../services/empresa.service";
@Component({
    selector: "empresas-editor",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./empresa.crud.component.html",
    styleUrl: "./empresa.crud.component.scss"
})
export class EmpresaCrudComponent {
    private fb = inject(FormBuilder);
    private api = inject(EmpresaService);
    private destroy = inject(DestroyRef);
    readonly empresa = input.required<Empresa>();
    readonly superusuario = input(false);
    readonly pendente = output<boolean>();
    readonly ocupado = output<boolean>();
    readonly salvo = output<Empresa>();
    readonly falha = output<unknown>();
    readonly processando = signal(false);
    readonly dadosEmpresa = this.fb.nonNullable.group({
        nome: ["", [Validators.required, Validators.maxLength(160)]],
        descricao: ["", Validators.maxLength(5000)],
        endereco: ["", Validators.maxLength(500)],
        contato: ["", Validators.maxLength(300)],
        site: ["", [Validators.maxLength(500), Validators.pattern(/^(https?:\/\/[^\s]+)?$/)]],
        servicos: ["", Validators.maxLength(10000)],
        fusoHorario: ["America/Sao_Paulo", [Validators.required, Validators.maxLength(80)]],
        revisao: [0]
    });
    private contexto = "";
    constructor() {
        this.dadosEmpresa.valueChanges
            .pipe(takeUntilDestroyed(this.destroy))
            .subscribe(() => this.pendente.emit(this.dadosEmpresa.dirty));
        effect(() => {
            const atual = this.empresa();
            const id = this.empresa().id;
            untracked(() => {
                if (this.contexto !== id || (!this.dadosEmpresa.dirty && !this.processando())) {
                    this.dadosEmpresa.reset(atual);
                    this.contexto = id;
                }
            });
        });
    }
    descartar(): void {
        this.dadosEmpresa.reset(this.empresa());
        this.pendente.emit(false);
    }
    async salvar(): Promise<void> {
        if (this.processando()) return;
        if (this.dadosEmpresa.invalid) {
            this.dadosEmpresa.markAllAsTouched();
            return;
        }
        const id = this.empresa().id;
        const dados = this.dadosEmpresa.getRawValue();
        this.processando.set(true);
        this.ocupado.emit(true);
        this.dadosEmpresa.disable({emitEvent: false});
        try {
            const atual = await firstValueFrom(this.api.salvar(id, dados));
            if (id !== this.empresa().id) return;
            this.dadosEmpresa.reset(atual);
            this.pendente.emit(false);
            this.salvo.emit(atual);
        } catch (erro) {
            if (id === this.empresa().id) this.falha.emit(erro);
        } finally {
            this.dadosEmpresa.enable({emitEvent: false});
            this.processando.set(false);
            this.ocupado.emit(false);
        }
    }
}
