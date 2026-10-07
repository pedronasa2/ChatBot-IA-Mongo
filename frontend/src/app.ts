import { Component, HostListener, inject, signal } from "@angular/core";
import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders,
} from "@angular/common/http";
import { ReactiveFormsModule, FormBuilder, Validators } from "@angular/forms";
import { firstValueFrom } from "rxjs";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_FORM_FIELD_DEFAULT_OPTIONS,
  MatFormFieldModule,
} from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { UiIconComponent } from "./ui-icon";
import { MatRadioModule } from "@angular/material/radio";
import { ComponentePainelSuperusuario } from "./painel-superusuario";
interface Empresa {
  id: string;
  nome: string;
  descricao: string;
  endereco: string;
  contato: string;
  site: string;
  servicos: string;
  fusoHorario: string;
  revisao: number;
  botAtivo: boolean;
}
interface RascunhoBot {
  nomeBot: string;
  tomVoz: string;
  usarEmojis: boolean;
  instrucoes: string;
  instrucoesTransferencia: string;
  revisao: number;
}
@Component({
  selector: "app-root",
  standalone: true,
  providers: [
    {
      provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: { subscriptSizing: "dynamic" },
    },
  ],
  imports: [
    UiIconComponent,
    ComponentePainelSuperusuario,
    MatRadioModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressBarModule,
  ],
  templateUrl: "./app.html",
})
export class ComponenteAplicacao {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);
  private csrf: { token: string; headerName: string } | null = null;
  readonly pronto = signal(false);
  readonly autenticado = signal(false);
  readonly processando = signal(false);
  readonly carregando = signal(false);
  readonly erro = signal("");
  readonly sucesso = signal("");
  readonly empresas = signal<{ id: string; nome: string; papel: string }[]>([]);
  readonly empresaSelecionada = signal("");
  readonly aba = signal("overview");
  readonly empresa = signal<Empresa | null>(null);
  readonly email = signal("");
  readonly superusuario = signal(false);
  readonly promptGlobalPendente = signal(false);
  readonly formularioLogin = this.fb.nonNullable.group({
    email: ["", [Validators.required, Validators.email]],
    senha: ["", Validators.required],
  });
  readonly dadosEmpresa = this.fb.nonNullable.group({
    nome: ["", [Validators.required, Validators.maxLength(160)]],
    descricao: ["", Validators.maxLength(5000)],
    endereco: ["", Validators.maxLength(500)],
    contato: ["", Validators.maxLength(300)],
    site: [
      "",
      [Validators.maxLength(500), Validators.pattern(/^(https?:\/\/[^\s]+)?$/)],
    ],
    servicos: ["", Validators.maxLength(10000)],
    fusoHorario: [
      "America/Sao_Paulo",
      [Validators.required, Validators.maxLength(80)],
    ],
    revisao: [0],
  });
  readonly bot = this.fb.nonNullable.group({
    nomeBot: ["", [Validators.required, Validators.maxLength(80)]],
    tomVoz: ["cordial", Validators.required],
    usarEmojis: [false],
    instrucoes: ["", Validators.maxLength(10000)],
    instrucoesTransferencia: ["", Validators.maxLength(5000)],
    revisao: [0],
  });
  constructor() {
    void this.inicializar();
  }
  private async inicializar() {
    try {
      await this.renovarCsrf();
      await this.carregarIdentidade();
      this.autenticado.set(true);
      await this.carregarEmpresas();
    } catch (e) {
      if (!(e instanceof HttpErrorResponse && e.status === 401))
        this.erro.set(
          "Não foi possível acessar o servidor. Confira a conexão e tente novamente.",
        );
    } finally {
      this.pronto.set(true);
    }
  }
  async renovarCsrf() {
    this.csrf = await firstValueFrom(
      this.http.get<{ token: string; headerName: string }>("/api/auth/csrf"),
    );
  }
  private async carregarIdentidade() {
    const usuario = await firstValueFrom(this.http.get<{ email: string; superusuario: boolean }>("/api/auth/me"));
    this.email.set(usuario.email);
    this.superusuario.set(usuario.superusuario);
  }
  async revalidarAcessoSuperusuario() {
    try {
      await this.carregarIdentidade();
      this.aba.set("overview");
      this.promptGlobalPendente.set(false);
      this.erro.set("Seu acesso à administração foi alterado. Confira sua conta.");
    } catch (erro) {
      await this.tratarFalha(erro);
    }
  }
  private cabecalhos() {
    if (!this.csrf) throw new Error("Sessão indisponível");
    return new HttpHeaders({ [this.csrf.headerName]: this.csrf.token });
  }
  async entrar() {
    if (this.formularioLogin.invalid || this.processando()) return;
    this.processando.set(true);
    this.erro.set("");
    try {
      await this.renovarCsrf();
      const b = this.formularioLogin.getRawValue();
      await firstValueFrom(
        this.http.post("/api/auth/login", new URLSearchParams(b).toString(), {
          headers: this.cabecalhos().set(
            "Content-Type",
            "application/x-www-form-urlencoded",
          ),
        }),
      );
      this.formularioLogin.controls.senha.reset();
      await this.renovarCsrf();
      await this.carregarIdentidade();
      this.autenticado.set(true);
      await this.carregarEmpresas();
    } catch (e) {
      this.erro.set(
        e instanceof HttpErrorResponse && e.status === 401
          ? "E-mail ou senha incorretos. Confira os dados."
          : "Não foi possível entrar. Confira a conexão e tente novamente.",
      );
    } finally {
      this.processando.set(false);
    }
  }
  async carregarEmpresas() {
    this.carregando.set(true);
    try {
      this.empresas.set(
        await firstValueFrom(
          this.http.get<{ id: string; nome: string; papel: string }[]>(
            "/api/empresas",
          ),
        ),
      );
      const first = this.empresas()[0];
      if (first) await this.carregarEmpresa(first.id);
    } catch (e) {
      this.tratarFalha(e);
    } finally {
      this.carregando.set(false);
    }
  }
  async carregarEmpresa(id: string) {
    this.empresaSelecionada.set(id);
    this.empresa.set(null);
    this.carregando.set(true);
    this.erro.set("");
    this.sucesso.set("");
    try {
      const [c, d] = await Promise.all([
        firstValueFrom(this.http.get<Empresa>(`/api/empresas/${id}`)),
        firstValueFrom(this.http.get<RascunhoBot>(`/api/empresas/${id}/rascunho`)),
      ]);
      if (this.empresaSelecionada() !== id) return;
      this.empresa.set(c);
      this.dadosEmpresa.reset(c);
      this.bot.reset(d);
    } catch (e) {
      this.tratarFalha(e);
    } finally {
      this.carregando.set(false);
    }
  }
  temAlteracoes() {
    return this.dadosEmpresa.dirty || this.bot.dirty || this.promptGlobalPendente();
  }
  trocarEmpresa(id: string) {
    if (
      (this.dadosEmpresa.dirty || this.bot.dirty) &&
      !window.confirm("Há alterações não salvas. Deseja descartá-las?")
    )
      return;
    void this.carregarEmpresa(id);
  }
  trocarAba(aba: string) {
    if (aba === "superusuario" && !this.superusuario()) return;
    if (this.aba() === "superusuario" && aba !== "superusuario" && this.promptGlobalPendente()
        && !window.confirm("O prompt global tem alterações não salvas. Deseja descartá-las?")) return;
    if (aba !== "superusuario") this.promptGlobalPendente.set(false);
    this.aba.set(aba);
    this.sucesso.set("");
    this.erro.set("");
  }
  async salvar(kind: "dadosEmpresa" | "bot") {
    const form = kind === "dadosEmpresa" ? this.dadosEmpresa : this.bot;
    if (form.invalid) {
      form.markAllAsTouched();
      this.erro.set("Revise os campos destacados antes de salvar.");
      return;
    }
    if (this.processando()) return;
    this.processando.set(true);
    this.erro.set("");
    this.sucesso.set("");
    try {
      if (kind === "dadosEmpresa") {
        const c = await firstValueFrom(
          this.http.put<Empresa>(
            `/api/empresas/${this.empresaSelecionada()}`,
            this.dadosEmpresa.getRawValue(),
            { headers: this.cabecalhos() },
          ),
        );
        this.empresa.set(c);
        this.dadosEmpresa.reset(c);
        this.empresas.update((list) =>
          list.map((x) => (x.id === c.id ? { ...x, nome: c.nome } : x)),
        );
      } else {
        const d = await firstValueFrom(
          this.http.put<RascunhoBot>(
            `/api/empresas/${this.empresaSelecionada()}/rascunho`,
            this.bot.getRawValue(),
            { headers: this.cabecalhos() },
          ),
        );
        this.bot.reset(d);
      }
      this.sucesso.set("Alterações salvas no rascunho.");
    } catch (e) {
      this.tratarFalha(e);
    } finally {
      this.processando.set(false);
    }
  }
  async sair() {
    if (
      this.temAlteracoes() &&
      !window.confirm("Há alterações não salvas. Deseja sair e descartá-las?")
    )
      return;
    this.processando.set(true);
    try {
      await firstValueFrom(
        this.http.post("/api/auth/logout", {}, { headers: this.cabecalhos() }),
      );
      this.limparSessao();
      this.sucesso.set("Você saiu do painel.");
      await this.renovarCsrf();
    } catch (e) {
      this.tratarFalha(e);
    } finally {
      this.processando.set(false);
    }
  }
  private limparSessao() {
    this.autenticado.set(false);
    this.superusuario.set(false);
    this.promptGlobalPendente.set(false);
    this.aba.set("overview");
    this.empresa.set(null);
    this.empresas.set([]);
    this.dadosEmpresa.reset();
    this.bot.reset();
  }
  private async tratarFalha(e: unknown) {
    if (e instanceof HttpErrorResponse && e.status === 401) {
      this.limparSessao();
      this.erro.set("Sua sessão foi encerrada. Entre novamente para continuar.");
      void this.renovarCsrf();
    } else if (e instanceof HttpErrorResponse && e.status === 403) {
      try {
        await firstValueFrom(this.http.get("/api/auth/me"));
        this.erro.set(
          "A sessão de segurança mudou. Recarregue a página e entre novamente.",
        );
      } catch (check) {
        if (check instanceof HttpErrorResponse && check.status === 401) {
          this.limparSessao();
          this.erro.set("Sua sessão foi encerrada. Entre novamente para continuar.");
          void this.renovarCsrf();
        } else {
          this.erro.set(
            "Não foi possível verificar sua sessão. Confira a conexão e tente novamente.",
          );
        }
      }
    } else if (e instanceof HttpErrorResponse && e.status === 404) {
      this.erro.set("Empresa indisponível para sua conta.");
    } else if (e instanceof HttpErrorResponse && e.status === 409) {
      this.erro.set(
        "Os dados foram alterados em outra sessão. Copie suas alterações e recarregue antes de salvar.",
      );
    } else {
      this.erro.set(
        e instanceof HttpErrorResponse && e.status === 400
          ? e.error?.mensagem || "Revise os dados informados."
          : "Não foi possível concluir. Confira a conexão e tente novamente.",
      );
    }
  }
  @HostListener("window:beforeunload", ["$event"]) antesFechar(
    event: BeforeUnloadEvent,
  ) {
    if (this.autenticado() && this.temAlteracoes()) {
      event.preventDefault();
      event.returnValue = "";
    }
  }
}
