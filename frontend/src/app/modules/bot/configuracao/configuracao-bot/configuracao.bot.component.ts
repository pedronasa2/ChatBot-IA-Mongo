import {Component, DestroyRef, effect, inject, input, output, signal, untracked} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormBuilder, Validators} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../../ui/ui.module";

import {RascunhoBot} from "../../classes/model/rascunho.bot";
import {ConfiguracaoBotService} from "../../services/configuracao.bot.service";

@Component({
    selector: "bot-editor",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./configuracao.bot.component.html",
    styleUrl: "./configuracao.bot.component.scss"
})
export class BotConfiguracaoComponent {
    private fb = inject(FormBuilder);
    private api = inject(ConfiguracaoBotService);
    private destroy = inject(DestroyRef);
    readonly empresaId = input.required<string>();
    readonly snapshot = input.required<RascunhoBot>();
    readonly superusuario = input(false);
    readonly pendente = output<boolean>();
    readonly ocupado = output<boolean>();
    readonly salvo = output<RascunhoBot>();
    readonly falha = output<unknown>();
    readonly processando = signal(false);
    readonly bot = this.fb.nonNullable.group({
        nomeBot: ["", [Validators.required, Validators.maxLength(80)]],
        tomVoz: ["cordial", Validators.required],
        usarEmojis: [false],
        instrucoes: ["", Validators.maxLength(10000)],
        instrucoesTransferencia: ["", Validators.maxLength(5000)],
        revisao: [0]
    });
    private contexto = "";

    constructor() {
        this.bot.valueChanges
            .pipe(takeUntilDestroyed(this.destroy))
            .subscribe(() => this.pendente.emit(this.bot.dirty));
        effect(() => {
            const atual = this.snapshot();
            const id = this.empresaId();
            untracked(() => {
                if (this.contexto !== id || (!this.bot.dirty && !this.processando())) {
                    this.bot.reset(atual);
                    this.contexto = id;
                }
            });
        });
    }

    descartar(): void {
        this.bot.reset(this.snapshot());
        this.pendente.emit(false);
    }
    async salvar(): Promise<void> {
        if (this.processando()) return;
        if (this.bot.invalid) {
            this.bot.markAllAsTouched();
            return;
        }
        const id = this.empresaId();
        const dados = this.bot.getRawValue();
        this.processando.set(true);
        this.ocupado.emit(true);
        this.bot.disable({emitEvent: false});
        try {
            const atual = await firstValueFrom(this.api.salvar(id, dados));
            if (id !== this.empresaId()) return;
            this.bot.reset(atual);
            this.pendente.emit(false);
            this.salvo.emit(atual);
        } catch (erro) {
            if (id === this.empresaId()) this.falha.emit(erro);
        } finally {
            this.bot.enable({emitEvent: false});
            this.processando.set(false);
            this.ocupado.emit(false);
        }
    }
}
