import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, input, signal} from "@angular/core";
import {FormBuilder, Validators} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../../ui/ui.module";
import {ConviteService} from "../../services/convite.service";

function mensagem(e: unknown): any {
    return e instanceof HttpErrorResponse
        ? e.error?.mensagem ||
              (e.status === 403
                  ? "Acesso exclusivo de superusuário. Entre novamente com a conta correta."
                  : "Não foi possível concluir. Tente novamente.")
        : "Não foi possível concluir. Tente novamente.";
}

@Component({
    selector: "ativar-convite",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./convite.ativacao.component.html",
    styleUrl: "./convite.ativacao.component.scss"
})
export class ConviteAtivacaoComponent {
    token = input.required<string>();
    private api = inject(ConviteService);
    private fb = inject(FormBuilder);
    ocupado = signal(false);
    concluido = signal(false);
    erro = signal("");
    form = this.fb.nonNullable.group({
        email: ["", [Validators.required, Validators.email]],
        senha: ["", [Validators.required, Validators.minLength(6), Validators.maxLength(21)]],
        confirmacaoSenha: ["", [Validators.required, Validators.minLength(6), Validators.maxLength(21)]]
    });

    async ativar(): Promise<void> {
        if (this.form.invalid || this.ocupado()) return;
        const d = this.form.getRawValue();
        if (d.senha !== d.confirmacaoSenha) {
            this.erro.set("As senhas não coincidem.");
            return;
        }
        this.ocupado.set(true);
        this.form.disable({emitEvent: false});
        this.erro.set("");
        try {
            await firstValueFrom(this.api.ativar({...d, token: this.token()}));
            this.form.reset();
            history.replaceState(null, "", "/");
            this.concluido.set(true);
        } catch (e) {
            this.erro.set(mensagem(e));
        } finally {
            this.form.enable({emitEvent: false});
            this.ocupado.set(false);
        }
    }
}
