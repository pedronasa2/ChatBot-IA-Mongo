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
        corPrimaria: ["#0d3b4c", [Validators.required, Validators.pattern(/^#[0-9a-fA-F]{6}$/)]],
        corSecundaria: ["#525e7f", [Validators.required, Validators.pattern(/^#[0-9a-fA-F]{6}$/)]],
        fotoEmpresa: [""],
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
    readonly erroFoto = signal("");
    restaurarCores(): void {
        this.dadosEmpresa.patchValue({corPrimaria: "#0d3b4c", corSecundaria: "#525e7f"});
        this.marcarAlteracao();
    }
    removerFoto(): void {
        this.dadosEmpresa.controls.fotoEmpresa.setValue("");
        this.marcarAlteracao();
    }
    private marcarAlteracao(): void {
        this.dadosEmpresa.markAsDirty();
        this.pendente.emit(true);
    }
    async selecionarFoto(event: Event): Promise<void> {
        const input = event.target as HTMLInputElement;
        const arquivo = input.files?.[0];
        input.value = "";
        if (!arquivo) return;
        this.erroFoto.set("");
        if (!["image/png", "image/jpeg"].includes(arquivo.type) || arquivo.size > 5 * 1024 * 1024) {
            this.erroFoto.set("Escolha uma imagem PNG ou JPEG de até 5 MB.");
            return;
        }
        const empresaId = this.empresa().id;
        try {
            const bitmap = await createImageBitmap(arquivo);
            const canvas = document.createElement("canvas");
            const escala = Math.min(1, 256 / Math.max(bitmap.width, bitmap.height));
            canvas.width = Math.max(1, Math.round(bitmap.width * escala));
            canvas.height = Math.max(1, Math.round(bitmap.height * escala));
            canvas.getContext("2d")!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
            bitmap.close();
            if (empresaId !== this.empresa().id || this.processando()) return;
            this.dadosEmpresa.controls.fotoEmpresa.setValue(canvas.toDataURL("image/png"));
            this.marcarAlteracao();
        } catch {
            this.erroFoto.set("Não foi possível abrir essa imagem. Escolha outro arquivo.");
        }
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
