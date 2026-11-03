import {HttpErrorResponse} from "@angular/common/http";
import {Component, effect, inject, input, output, signal} from "@angular/core";
import {FormBuilder, Validators} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../../ui/ui.module";

import {VinculoChatwootService} from "../../services/vinculo.chatwoot.service";

@Component({
    selector: "vinculo-chatwoot",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./vinculo.chatwoot.component.html",
    styleUrl: "./vinculo.chatwoot.component.scss"
})
export class VinculoChatwootComponent {
    empresaId = input.required<string>();
    salvo = output<void>();
    private api = inject(VinculoChatwootService);
    private fb = inject(FormBuilder);
    carregando = signal(false);
    ocupado = signal(false);
    erro = signal("");
    sucesso = signal(false);
    private sequencia = 0;
    form = this.fb.group({
        accountId: this.fb.control<number | null>(null, [
            Validators.required,
            Validators.min(1),
            Validators.max(Number.MAX_SAFE_INTEGER),
            Validators.pattern(/^[0-9]+$/)
        ]),
        inboxId: this.fb.control<number | null>(null, [
            Validators.required,
            Validators.min(1),
            Validators.max(Number.MAX_SAFE_INTEGER),
            Validators.pattern(/^[0-9]+$/)
        ])
    });

    constructor() {
        effect(() => {
            const id = this.empresaId();
            void this.carregar(id);
        });
    }

    private async carregar(id: string): Promise<void> {
        const seq = ++this.sequencia;
        this.form.reset();
        this.erro.set("");
        this.sucesso.set(false);
        this.carregando.set(true);
        this.form.disable();
        try {
            if (id) {
                const d = await firstValueFrom(this.api.consultar(id));
                if (seq === this.sequencia) this.form.reset(d);
            }
        } catch {
            if (seq === this.sequencia) this.erro.set("Não foi possível carregar o vínculo com Chatwoot.");
        } finally {
            if (seq === this.sequencia) {
                this.carregando.set(false);
                if (id) this.form.enable();
            }
        }
    }

    descartar(): void {
        this.form.reset();
    }
    async salvar(): Promise<void> {
        if (this.form.invalid || this.carregando() || this.ocupado() || !this.empresaId()) return;
        const id = this.empresaId(),
            dados = this.form.getRawValue();
        this.form.disable({emitEvent: false});
        this.ocupado.set(true);
        this.erro.set("");
        this.sucesso.set(false);
        try {
            if (dados.accountId === null || dados.inboxId === null) return;
            await firstValueFrom(this.api.salvar(id, {accountId: dados.accountId, inboxId: dados.inboxId}));
            if (id === this.empresaId()) {
                this.sucesso.set(true);
                this.form.markAsPristine();
                this.salvo.emit();
            }
        } catch (e) {
            if (id === this.empresaId())
                this.erro.set(
                    e instanceof HttpErrorResponse && e.status === 409
                        ? "Esta conta e caixa já estão vinculadas a outra empresa."
                        : "Não foi possível salvar o vínculo. Confira os dados e tente novamente."
                );
        } finally {
            this.form.enable({emitEvent: false});
            this.ocupado.set(false);
        }
    }
}
