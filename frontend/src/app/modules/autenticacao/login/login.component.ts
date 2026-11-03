import {UiModule} from "../../ui/ui.module";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, input, output, signal} from "@angular/core";
import {FormBuilder, Validators} from "@angular/forms";

import {SessaoService} from "../../core/security/sessao.service";

@Component({
    selector: "login-page",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./login.component.html",
    styleUrl: "./login.component.scss"
})
export class LoginComponent {
    private fb = inject(FormBuilder);
    private sessao = inject(SessaoService);
    readonly entrou = output<void>();
    readonly mensagem = input("");
    readonly sucesso = input("");
    readonly erro = signal("");
    readonly processando = signal(false);
    readonly formularioLogin = this.fb.nonNullable.group({
        email: ["", [Validators.required, Validators.email]],
        senha: ["", Validators.required]
    });
    async entrar(): Promise<void> {
        if (this.formularioLogin.invalid || this.processando()) return;
        this.processando.set(true);
        this.erro.set("");
        try {
            await this.sessao.entrar(this.formularioLogin.getRawValue());
            this.formularioLogin.controls.senha.reset();
            this.entrou.emit();
        } catch (e) {
            this.erro.set(
                e instanceof HttpErrorResponse && e.status === 401
                    ? "E-mail ou senha incorretos. Confira os dados."
                    : "Não foi possível entrar. Confira a conexão e tente novamente."
            );
        } finally {
            this.processando.set(false);
        }
    }
}
