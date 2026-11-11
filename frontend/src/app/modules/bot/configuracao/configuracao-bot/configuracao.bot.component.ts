import {Component, DestroyRef, effect, inject, input, output, signal, untracked} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormBuilder, Validators} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../../ui/ui.module";

import {Empresa} from "../../../empresas/classes/model/empresa";
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
    readonly snapshot = input.required<Empresa>();
    readonly superusuario = input(false);
    readonly pendente = output<boolean>();
    readonly ocupado = output<boolean>();
    readonly salvo = output<Empresa>();
    readonly falha = output<unknown>();
    readonly processando = signal(false);
    readonly bot = this.fb.nonNullable.group({
        nome: ["", [Validators.required, Validators.maxLength(80)]],
        tomVoz: ["cordial", Validators.required],
        usarEmojis: [false],
        instrucoes: ["", Validators.maxLength(10000)],
        instrucoesTransferencia: ["", Validators.maxLength(5000)],
        modoGuiado: [false],
        questionario: this.fb.nonNullable.group({
            pagamentos: ["", Validators.maxLength(1500)], entrega: ["", Validators.maxLength(1500)],
            horarios: ["", Validators.maxLength(1500)], perguntasFrequentes: ["", Validators.maxLength(3000)],
            chamarPessoa: ["", Validators.maxLength(1500)]
        }),
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
                    this.bot.reset({...atual.bot, instrucoes: atual.bot.instrucoesLivres ?? atual.bot.instrucoes, instrucoesTransferencia: atual.bot.instrucoesTransferenciaLivres ?? atual.bot.instrucoesTransferencia, revisao: atual.revisao});
                    this.contexto = id;
                }
            });
        });
    }

    previaOrientacoes(): string {
        const dados = this.bot.getRawValue();
        if (!dados.modoGuiado) return dados.instrucoes || "Nenhuma instrução adicional definida.";
        const q = dados.questionario;
        return [
            `Use os dados de ${this.snapshot().nome} já cadastrados em Meu negócio. Não invente informações ausentes.`,
            `Estilo: ${dados.tomVoz}. Emojis: ${dados.usarEmojis ? "permitidos" : "não usar"}.`,
            ...[["Pagamento", q.pagamentos], ["Entrega e retirada", q.entrega], ["Horários", q.horarios],
                ["Perguntas frequentes", q.perguntasFrequentes], ["Quando chamar uma pessoa", q.chamarPessoa]]
                .filter(([, valor]) => valor?.trim()).map(([nome, valor]) => `${nome}: ${valor.trim()}`),
            dados.instrucoes ? `Orientações adicionais: ${dados.instrucoes}` : ""
        ].filter(Boolean).join("\n\n");
    }
    descartar(): void {
        this.bot.reset({...this.snapshot().bot, instrucoes: this.snapshot().bot.instrucoesLivres ?? this.snapshot().bot.instrucoes, instrucoesTransferencia: this.snapshot().bot.instrucoesTransferenciaLivres ?? this.snapshot().bot.instrucoesTransferencia, revisao: this.snapshot().revisao});
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
            this.bot.reset({...atual.bot, instrucoes: atual.bot.instrucoesLivres ?? atual.bot.instrucoes, instrucoesTransferencia: atual.bot.instrucoesTransferenciaLivres ?? atual.bot.instrucoesTransferencia, revisao: atual.revisao});
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
