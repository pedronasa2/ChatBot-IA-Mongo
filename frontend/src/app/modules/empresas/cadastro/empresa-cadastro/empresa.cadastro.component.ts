import {HttpErrorResponse} from "@angular/common/http";
import {Component, effect, inject, input, output, signal, untracked} from "@angular/core";
import {FormBuilder, Validators} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {ConviteCriado as Convite} from "../../../convites/classes/vo/convite.criado";
import {SituacaoConvite as Situacao} from "../../../convites/classes/vo/situacao.convite";
import {ConviteService} from "../../../convites/services/convite.service";
import {UiModule} from "../../../ui/ui.module";

function mensagem(e: unknown): any {
    return e instanceof HttpErrorResponse
        ? e.error?.mensagem ||
              (e.status === 403
                  ? "Acesso exclusivo de superusuário. Entre novamente com a conta correta."
                  : "Não foi possível concluir. Tente novamente.")
        : "Não foi possível concluir. Tente novamente.";
}

@Component({
    selector: "cadastro-negocio",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./empresa.cadastro.component.html",
    styleUrl: "./empresa.cadastro.component.scss"
})
export class EmpresaCadastroComponent {
    empresaId = input("");
    criada = output<string>();
    private api = inject(ConviteService);
    private fb = inject(FormBuilder);
    aberto = signal(false);
    ocupado = signal(false);
    erro = signal("");
    convite = signal<Convite | null>(null);
    situacao = signal<Situacao | null>(null);
    copiado = signal(false);
    form = this.fb.nonNullable.group({
        nome: ["", [Validators.required, Validators.maxLength(160)]],
        responsavel: ["", [Validators.required, Validators.maxLength(160)]],
        email: ["", [Validators.required, Validators.email, Validators.maxLength(254)]],
    });

    constructor() {
        effect(() => {
            const id = this.empresaId();
            untracked(() => {
                this.situacao.set(null);
                if (this.convite()?.empresaId !== id) this.convite.set(null);
                if (id) void this.consultar(id);
            });
        });
    }

    selecionar(e: Event): void {
        (e.target as HTMLInputElement).select();
    }

    link(): string {
        return this.convite() ? window.location.origin + "/#convite=" + encodeURIComponent(this.convite()!.token) : "";
    }

    async copiar(): Promise<void> {
        try {
            await navigator.clipboard.writeText(this.link());
            this.copiado.set(true);
        } catch {
            this.erro.set("Selecione o link e copie manualmente.");
        }
    }

    async consultar(id = this.empresaId()): Promise<void> {
        try {
            const s = await firstValueFrom(this.api.consultar(id));
            if (id === this.empresaId()) {
                this.situacao.set(s);
                if (s.estado === "aceito") this.convite.set(null);
            }
        } catch (e) {
            if (id === this.empresaId()) this.erro.set(mensagem(e));
        }
    }

    descartar(): void {
        this.form.reset();
    }
    async criar(): Promise<void> {
        if (this.form.invalid || this.ocupado()) return;
        this.ocupado.set(true);
        this.form.disable({emitEvent: false});
        this.erro.set("");
        try {
            const c = await firstValueFrom(this.api.criar(this.form.getRawValue()));
            this.aberto.set(false);
            this.form.reset();
            this.criada.emit(c.empresaId);
            this.convite.set(c);
            this.copiado.set(false);
        } catch (e) {
            this.erro.set(mensagem(e));
        } finally {
            this.form.enable({emitEvent: false});
            this.ocupado.set(false);
        }
    }

    async reenviar(): Promise<void> {
        if (this.ocupado()) return;
        this.ocupado.set(true);
        this.erro.set("");
        try {
            const id = this.empresaId();
            const c = await firstValueFrom(this.api.reenviar(id));
            if (id === this.empresaId()) {
                this.convite.set(c);
                this.copiado.set(false);
                await this.consultar(id);
            }
        } catch (e) {
            this.erro.set(mensagem(e));
        } finally {
            this.form.enable({emitEvent: false});
            this.ocupado.set(false);
        }
    }
}
