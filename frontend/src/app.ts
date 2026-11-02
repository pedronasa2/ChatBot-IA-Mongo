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
import { Empresa, ModuloEmpresa } from './modelos-empresa';
import { ServicoAcessoModulos } from './acesso-modulos';
import { EstadoEdicaoModulo } from './estado-edicao-modulo';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { DestroyRef, effect, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
    RouterOutlet,
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
  private readonly router = inject(Router);
  private readonly destruidor = inject(DestroyRef);
  private readonly acessoModulos = inject(ServicoAcessoModulos);
  private readonly edicaoModulo = inject(EstadoEdicaoModulo);
  readonly empresaRota = signal('');
  private consultandoPermissoes = false;
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
  readonly modulosEmpresa = this.fb.nonNullable.group({
    moduloCandidatosHabilitado: [false], moduloAgendaHabilitado: [false], revisao: [0],
  });
  readonly salvandoModulos = signal(false);
  readonly bot = this.fb.nonNullable.group({
    nomeBot: ["", [Validators.required, Validators.maxLength(80)]],
    tomVoz: ["cordial", Validators.required],
    usarEmojis: [false],
    instrucoes: ["", Validators.maxLength(10000)],
    instrucoesTransferencia: ["", Validators.maxLength(5000)],
    revisao: [0],
  });
  constructor() {
    effect(() => {
      const verificada = this.acessoModulos.empresaVerificada();
      if (verificada) untracked(() => {
        if (this.empresaSelecionada() === verificada.id && this.empresa()) {
          this.empresa.set(verificada);
          if (!this.dadosEmpresa.dirty) this.dadosEmpresa.reset(verificada);
          if (!this.modulosEmpresa.dirty) this.modulosEmpresa.reset(verificada);
        }
      });
    });
    effect(() => {
      if (this.acessoModulos.sessaoEncerrada()) {
        this.acessoModulos.sessaoEncerrada.set(false);
        void this.tratarFalha(new HttpErrorResponse({ status: 401 }));
      }
      const negacao = this.acessoModulos.negacao();
      if (negacao) {
        this.erro.set(negacao);
        untracked(() => void this.atualizarPermissoesEmpresa());
      }
    });
    this.router.events.pipe(takeUntilDestroyed(this.destruidor)).subscribe(evento => {
      if (!(evento instanceof NavigationEnd)) return;
      const rota = this.moduloNaUrl();
      this.empresaRota.set(rota?.empresaId || '');
      if (rota) {
        this.aba.set(rota.modulo);
        if (this.autenticado() && this.empresaSelecionada() !== rota.empresaId) void this.carregarEmpresa(rota.empresaId);
      } else if (this.aba() === 'candidatos' || this.aba() === 'agenda') this.aba.set('overview');
    });
    const intervalo = setInterval(() => { if (!document.hidden) void this.atualizarPermissoesEmpresa(); }, 15000);
    this.destruidor.onDestroy(() => clearInterval(intervalo));
    void this.inicializar();
  }
  private moduloNaUrl() {
    const resultado = this.router.url.match(/^\/empresas\/([^/]+)\/(candidatos|agenda)(?:[?#]|$)/);
    return resultado ? { empresaId: resultado[1], modulo: resultado[2] as ModuloEmpresa } : null;
  }
  abrirModulo(modulo: ModuloEmpresa) {
    if (this.temAlteracoes() && !window.confirm('Há alterações não salvas. Deseja descartá-las?')) return;
    void this.router.navigate(['/empresas', this.empresaSelecionada(), modulo]);
  }
  @HostListener('window:focus') aoRetornarParaJanela() { void this.atualizarPermissoesEmpresa(); }
  async atualizarPermissoesEmpresa() {
    if (!this.autenticado() || !this.empresa() || this.consultandoPermissoes || this.carregando()) return;
    const id = this.empresaSelecionada();
    this.consultandoPermissoes = true;
    try {
      const atual = await firstValueFrom(this.http.get<Empresa>(`/api/empresas/${id}`));
      if (id !== this.empresaSelecionada()) return;
      this.empresa.set(atual);
      if (!this.dadosEmpresa.dirty) this.dadosEmpresa.reset(atual);
      if (!this.modulosEmpresa.dirty) this.modulosEmpresa.reset(atual);
      if ((this.aba() === 'candidatos' && !atual.moduloCandidatosHabilitado)
          || (this.aba() === 'agenda' && !atual.moduloAgendaHabilitado)) {
        this.erro.set('Este módulo foi desabilitado para a empresa. Os registros foram preservados.');
        void this.router.navigateByUrl('/');
      }
    } catch (erro) {
      if (erro instanceof HttpErrorResponse && (erro.status === 401 || erro.status === 404)) {
        this.empresa.set(null);
        void this.router.navigateByUrl('/');
        await this.tratarFalha(erro);
      }
    } finally { this.consultandoPermissoes = false; }
  }
  async salvarPermissoesModulos() {
    if (!this.superusuario() || this.salvandoModulos() || !this.modulosEmpresa.dirty) return;
    const id = this.empresaSelecionada();
    const dados = this.modulosEmpresa.getRawValue();
    this.modulosEmpresa.disable({ emitEvent: false });
    this.salvandoModulos.set(true);
    this.erro.set(''); this.sucesso.set('');
    try {
      await this.renovarCsrf();
      if (id !== this.empresaSelecionada()) return;
      const empresa = await firstValueFrom(this.http.put<Empresa>(`/api/superusuario/empresas/${id}/modulos`,
        dados, { headers: this.cabecalhos() }));
      if (id !== this.empresaSelecionada()) return;
      this.empresa.set(empresa);
      this.modulosEmpresa.reset(empresa);
      this.dadosEmpresa.controls.revisao.setValue(empresa.revisao);
      this.sucesso.set('Permissões dos módulos salvas para esta empresa.');
    } catch (erro) { await this.tratarFalha(erro); }
    finally { this.modulosEmpresa.enable({ emitEvent: false }); this.salvandoModulos.set(false); }
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
      const solicitada = this.moduloNaUrl()?.empresaId;
      const first = this.empresas().find(empresa => empresa.id === solicitada) || this.empresas()[0];
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
      this.modulosEmpresa.reset(c);
      const rota = this.moduloNaUrl();
      if (rota && rota.empresaId !== id) {
        const habilitado = rota.modulo === 'candidatos' ? c.moduloCandidatosHabilitado : c.moduloAgendaHabilitado;
        void this.router.navigateByUrl(habilitado ? `/empresas/${id}/${rota.modulo}` : '/');
      }
      this.dadosEmpresa.reset(c);
      this.bot.reset(d);
    } catch (e) {
      this.tratarFalha(e);
    } finally {
      this.carregando.set(false);
    }
  }
  temAlteracoes() {
    return this.dadosEmpresa.dirty || this.bot.dirty || this.promptGlobalPendente() || this.modulosEmpresa.dirty || this.edicaoModulo.pendente();
  }
  trocarEmpresa(id: string) {
    if (
      (this.dadosEmpresa.dirty || this.bot.dirty || this.modulosEmpresa.dirty || this.edicaoModulo.pendente()) &&
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
    if (this.edicaoModulo.pendente() && !window.confirm('Há alterações não salvas neste registro. Deseja descartá-las?')) return;
    void this.router.navigateByUrl('/').then(() => this.aba.set(aba));
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
    const id = this.empresaSelecionada();
    this.processando.set(true);
    this.erro.set("");
    this.sucesso.set("");
    try {
      if (kind === "dadosEmpresa") {
        const c = await firstValueFrom(
          this.http.put<Empresa>(
            `/api/empresas/${id}`,
            this.dadosEmpresa.getRawValue(),
            { headers: this.cabecalhos() },
          ),
        );
        if (id !== this.empresaSelecionada()) return;
        this.empresa.set(c);
        this.dadosEmpresa.reset(c);
        if (this.modulosEmpresa.dirty) this.modulosEmpresa.controls.revisao.setValue(c.revisao);
        else this.modulosEmpresa.reset(c);
        this.empresas.update((list) =>
          list.map((x) => (x.id === c.id ? { ...x, nome: c.nome } : x)),
        );
      } else {
        const d = await firstValueFrom(
          this.http.put<RascunhoBot>(
            `/api/empresas/${id}/rascunho`,
            this.bot.getRawValue(),
            { headers: this.cabecalhos() },
          ),
        );
        if (id !== this.empresaSelecionada()) return;
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
    void this.router.navigateByUrl('/');
    this.autenticado.set(false);
    this.superusuario.set(false);
    this.promptGlobalPendente.set(false);
    this.aba.set("overview");
    this.empresa.set(null);
    this.empresas.set([]);
    this.modulosEmpresa.reset();
    this.edicaoModulo.pendente.set(false);
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
