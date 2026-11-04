import {HttpErrorResponse} from "@angular/common/http";
import {Component, computed, effect, inject, input, output, signal, untracked} from "@angular/core";
import {FormBuilder, Validators} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../../ui/ui.module";
import {EmpresaService} from "../../../empresas/services/empresa.service";
import {Empresa} from "../../../empresas/classes/model/empresa";
@Component({
    selector: "vinculo-chatwoot",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./chatwoot.component.html",
    styleUrl: "./chatwoot.component.scss"
})
export class ChatwootComponent {
    readonly empresa = input.required<Empresa>();
    readonly empresaId = computed(() => this.empresa().id);
    readonly salvo = output<Empresa>();
    private readonly api = inject(EmpresaService);
    private readonly fb = inject(FormBuilder);
    readonly ocupado = signal(false);
    readonly erro = signal("");
    readonly sucesso = signal(false);
    private contexto = "";
    private revisao = 0;
    readonly form = this.fb.group({
        accountId: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(Number.MAX_SAFE_INTEGER), Validators.pattern(/^[0-9]+$/)]),
        inboxId: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(Number.MAX_SAFE_INTEGER), Validators.pattern(/^[0-9]+$/)])
    });
    constructor() {
        effect(() => {
            const atual = this.empresa();
            untracked(() => {
                const mudouEmpresa = this.contexto !== atual.id;
                if (mudouEmpresa || (!this.form.dirty && !this.ocupado())) {
                    this.form.reset({accountId: atual.chatwootAccountId, inboxId: atual.chatwootInboxId});
                    this.revisao = atual.revisao;
                    this.contexto = atual.id;
                    if (mudouEmpresa) {
                        this.erro.set("");
                        this.sucesso.set(false);
                    }
                }
            });
        });
    }
    descartar(): void {
        const atual = this.empresa();
        this.form.reset({accountId: atual.chatwootAccountId, inboxId: atual.chatwootInboxId});
        this.revisao = atual.revisao;
    }
    async salvar(): Promise<void> {
        if (this.form.invalid || this.ocupado() || !this.empresaId()) return;
        const id = this.empresaId();
        const dados = this.form.getRawValue();
        if (dados.accountId === null || dados.inboxId === null) return;
        this.form.disable({emitEvent: false});
        this.ocupado.set(true);
        this.erro.set("");
        this.sucesso.set(false);
        try {
            const atual = await firstValueFrom(this.api.salvarChatwoot(id, {
                chatwootAccountId: dados.accountId, chatwootInboxId: dados.inboxId, revisao: this.revisao
            }));
            if (id === this.empresaId()) {
                this.revisao = atual.revisao;
                this.form.markAsPristine();
                this.salvo.emit(atual);
                this.sucesso.set(true);
            }
        } catch (erro) {
            if (id === this.empresaId()) this.erro.set(erro instanceof HttpErrorResponse && erro.error?.mensagem
                ? erro.error.mensagem : "Não foi possível salvar o Chatwoot. Tente novamente.");
        } finally {
            this.form.enable({emitEvent: false});
            this.ocupado.set(false);
        }
    }
}
