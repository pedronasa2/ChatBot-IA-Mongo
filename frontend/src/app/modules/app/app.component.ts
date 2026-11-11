import {HttpErrorResponse} from "@angular/common/http";
import {Component, DestroyRef, effect, HostListener, inject, signal, ViewChild} from "@angular/core";
import {firstValueFrom} from "rxjs";
import {SaidaEditoresService} from "../core/security/saida.editores.service";
import {UiModule} from "../ui/ui.module";

import {MAT_FORM_FIELD_DEFAULT_OPTIONS} from "@angular/material/form-field";

import {MatSelect} from "@angular/material/select";

import {Router} from "@angular/router";
import {LoginComponent} from "../autenticacao/login/login.component";
import {PainelSuperusuarioComponent} from "../administracao/painel-superusuario/painel.superusuario.component";
import {BotConfiguracaoComponent} from "../bot/configuracao/configuracao-bot/configuracao.bot.component";
import {ChatwootComponent} from "../chatwoot/configuracao/chatwoot/chatwoot.component";
import {ConviteAtivacaoComponent} from "../convites/ativacao/convite-ativacao/convite.ativacao.component";
import {SessaoService} from "../core/security/sessao.service";
import {EmpresaCadastroComponent} from "../empresas/cadastro/empresa-cadastro/empresa.cadastro.component";
import {EmpresaCrudComponent} from "../empresas/cadastro/empresa-crud/empresa.crud.component";
import {Empresa} from "../empresas/classes/model/empresa";
import {EmpresaService} from "../empresas/services/empresa.service";

@Component({
    selector: "app-root",
    standalone: true,
    providers: [
        {
            provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
            useValue: {subscriptSizing: "dynamic"}
        }
    ],
    imports: [
        UiModule,
        EmpresaCadastroComponent,
        ConviteAtivacaoComponent,
        ChatwootComponent,
        LoginComponent,
        EmpresaCrudComponent,
        BotConfiguracaoComponent,
        PainelSuperusuarioComponent
    ],
    templateUrl: "./app.component.html",
    styleUrl: "./app.component.scss"
})
export class AppComponent {
    readonly tokenConvite = new URLSearchParams(window.location.hash.slice(1)).get("convite") || "";

    async empresaCriada(id: string): Promise<void> {
        this.empresaSelecionada.set(id);
        await this.carregarEmpresas(id);
        this.aba.set("dadosEmpresa");
    }

    private readonly saida = inject(SaidaEditoresService);
    @ViewChild(EmpresaCrudComponent) editorEmpresa?: EmpresaCrudComponent;
    @ViewChild(BotConfiguracaoComponent) editorBot?: BotConfiguracaoComponent;
    @ViewChild(EmpresaCadastroComponent) cadastro?: EmpresaCadastroComponent;
    @ViewChild(ChatwootComponent) chatwoot?: ChatwootComponent;
    private outrasAlteracoes(): boolean {
        return (
            this.empresaPendente() ||
            this.botPendente() ||
            this.promptGlobalPendente() ||
            !!this.cadastro?.form.dirty ||
            !!this.chatwoot?.form.dirty
        );
    }
    private mutacaoPendente(): boolean {
        return (
            this.salvandoEmpresa() ||
            this.salvandoBot() ||
            !!this.cadastro?.ocupado() ||
            !!this.chatwoot?.ocupado()
        );
    }
    private autorizarSaida(): boolean {
        if (!this.autenticado()) return true;
        if (this.mutacaoPendente()) return false;
        if (!this.outrasAlteracoes()) return true;
        if (!window.confirm("Há alterações não salvas. Deseja descartá-las?")) return false;
        this.editorEmpresa?.descartar();
        this.editorBot?.descartar();
        this.cadastro?.descartar();
        this.chatwoot?.descartar();
        this.promptGlobalPendente.set(false);
        return true;
    }
    private readonly router = inject(Router);
    private readonly destruidor = inject(DestroyRef);
    private consultandoEmpresa = false;
    private sessao = inject(SessaoService);
    private apiEmpresas = inject(EmpresaService);
    readonly pronto = signal(false);
    readonly autenticado = signal(false);
    readonly processando = signal(false);
    readonly carregando = signal(false);
    readonly erro = signal("");
    readonly sucesso = signal("");
    readonly empresas = signal<{id: string; nome: string; papel: string}[]>([]);
    readonly empresaSelecionada = signal("");
    readonly aba = signal("overview");
    readonly empresa = signal<Empresa | null>(null);
    readonly email = signal("");
    readonly superusuario = signal(false);
    readonly promptGlobalPendente = signal(false);
    readonly empresaPendente = signal(false);
    readonly botPendente = signal(false);
    readonly salvandoEmpresa = signal(false);
    readonly salvandoBot = signal(false);

    constructor() {
        effect(() => {
            const empresa = this.autenticado() ? this.empresa() : null;
            const primaria = empresa?.corPrimaria || "#0d3b4c";
            const secundaria = empresa?.corSecundaria || "#525e7f";
            const contraste = (cor: string) => {
                const rgb = [1, 3, 5].map(i => parseInt(cor.slice(i, i + 2), 16) / 255)
                    .map(c => c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
                return rgb[0] * 0.2126 + rgb[1] * 0.7152 + rgb[2] * 0.0722 > 0.179 ? "#000000" : "#ffffff";
            };
            const style = document.documentElement.style;
            style.setProperty("--primary", primaria);
            style.setProperty("--mat-sys-primary", primaria);
            style.setProperty("--mat-sys-on-primary", contraste(primaria));
            style.setProperty("--mat-sys-secondary", secundaria);
            style.setProperty("--mat-sys-on-secondary", contraste(secundaria));
            style.setProperty("--secondary", secundaria);
        });
        this.saida.registrar(() => this.autorizarSaida());
        this.destruidor.onDestroy(() => this.saida.limpar());
        const intervalo = setInterval(() => {
            if (!document.hidden) void this.atualizarEmpresa();
        }, 15000);
        this.destruidor.onDestroy(() => clearInterval(intervalo));
        if (this.tokenConvite) this.pronto.set(true);
        else void this.inicializar();
    }

    @HostListener("window:focus") aoRetornarParaJanela(): void {
        void this.atualizarEmpresa();
    }

    async atualizarEmpresa(): Promise<void> {
        if (!this.autenticado() || !this.empresa() || this.consultandoEmpresa || this.carregando()) return;
        const id = this.empresaSelecionada();
        this.consultandoEmpresa = true;
        try {
            const atual = await firstValueFrom(this.apiEmpresas.consultar(id));
            if (id !== this.empresaSelecionada()) return;
            this.empresa.set(atual);
        } catch (erro) {
            if (id !== this.empresaSelecionada()) return;
            if (erro instanceof HttpErrorResponse && (erro.status === 401 || erro.status === 404)) {
                this.empresa.set(null);
                void this.router.navigateByUrl("/");
                await this.tratarFalha(erro);
            }
        } finally {
            this.consultandoEmpresa = false;
        }
    }

    private async inicializar(): Promise<void> {
        try {
            await this.renovarCsrf();
            await this.carregarIdentidade();
            this.autenticado.set(true);
            await this.carregarEmpresas();
        } catch (e) {
            if (!(e instanceof HttpErrorResponse && e.status === 401))
                this.erro.set("Não foi possível acessar o servidor. Confira a conexão e tente novamente.");
        } finally {
            this.pronto.set(true);
        }
    }

    async renovarCsrf(): Promise<void> {
        await this.sessao.renovarCsrf();
    }

    private async carregarIdentidade(): Promise<void> {
        const usuario = await this.sessao.identidade();
        this.email.set(usuario.email);
        this.superusuario.set(usuario.perfil === "SUPERUSUARIO");
    }

    async revalidarAcessoSuperusuario(): Promise<void> {
        try {
            await this.carregarIdentidade();
            this.aba.set("overview");
            this.promptGlobalPendente.set(false);
            this.erro.set("Seu acesso à administração foi alterado. Confira sua conta.");
        } catch (erro) {
            await this.tratarFalha(erro);
        }
    }

    async aposLogin(): Promise<void> {
        await this.carregarIdentidade();
        this.autenticado.set(true);
        this.erro.set("");
        await this.carregarEmpresas();
    }

    async carregarEmpresas(preferida?: string): Promise<void> {
        this.carregando.set(true);
        try {
            this.empresas.set(await firstValueFrom(this.apiEmpresas.listar()));
            const selecionada = this.empresas().find((empresa) => empresa.id === preferida) || this.empresas()[0];
            if (selecionada) await this.carregarEmpresa(selecionada.id);
        } catch (e) {
            void this.tratarFalha(e);
        } finally {
            this.carregando.set(false);
        }
    }

    private geracaoEmpresa = 0;

    async carregarEmpresa(id: string): Promise<void> {
        const geracao = ++this.geracaoEmpresa;
        this.empresaSelecionada.set(id);
        this.empresa.set(null);
        this.carregando.set(true);
        this.erro.set("");
        this.sucesso.set("");
        try {
            const c = await firstValueFrom(this.apiEmpresas.consultar(id));
            if (this.empresaSelecionada() !== id || geracao !== this.geracaoEmpresa) return;
            this.empresa.set(c);
        } catch (e) {
            if (geracao === this.geracaoEmpresa) void this.tratarFalha(e);
        } finally {
            if (geracao === this.geracaoEmpresa) this.carregando.set(false);
        }
    }

    temAlteracoes(): boolean {
        return this.outrasAlteracoes();
    }

    async trocarEmpresa(id: string, seletor?: MatSelect): Promise<void> {
        if (!this.autorizarSaida()) {
            if (seletor) seletor.value = this.empresaSelecionada();
            return;
        }
        await this.carregarEmpresa(id);
    }

    trocarAba(aba: string): void {
        if (aba === "superusuario" && !this.superusuario()) return;
        if (
            this.aba() === "superusuario" &&
            aba !== "superusuario" &&
            this.promptGlobalPendente() &&
            !window.confirm("O prompt global tem alterações não salvas. Deseja descartá-las?")
        )
            return;
        if (aba !== "superusuario") this.promptGlobalPendente.set(false);
        if (this.router.url === "/") this.aba.set(aba);
        else
            void this.router.navigateByUrl("/").then((ok) => {
                if (ok) this.aba.set(aba);
            });
        this.sucesso.set("");
        this.erro.set("");
    }

    empresaSalva(atual: Empresa): void {
        this.empresa.set(atual);
        this.empresas.update((lista) => lista.map((e) => (e.id === atual.id ? {...e, nome: atual.nome} : e)));
        this.sucesso.set("Alterações salvas.");
    }

    botSalvo(atual: Empresa): void {
        this.empresa.set(atual);
        this.sucesso.set("Alterações salvas.");
    }

    async sair(): Promise<void> {
        if (this.temAlteracoes() && !window.confirm("Há alterações não salvas. Deseja sair e descartá-las?")) return;
        this.processando.set(true);
        try {
            await this.sessao.sair();
            this.limparSessao();
            this.sucesso.set("Você saiu do painel.");
            await this.renovarCsrf();
        } catch (e) {
            this.tratarFalha(e);
        } finally {
            this.processando.set(false);
        }
    }

    private limparSessao(): void {
        ++this.geracaoEmpresa;
        void this.router.navigateByUrl("/");
        this.autenticado.set(false);
        this.superusuario.set(false);
        this.promptGlobalPendente.set(false);
        this.aba.set("overview");
        this.empresa.set(null);
        this.empresas.set([]);
        this.empresaPendente.set(false);
        this.botPendente.set(false);
    }

    async tratarFalha(e: unknown): Promise<void> {
        if (e instanceof HttpErrorResponse && e.status === 401) {
            this.limparSessao();
            this.erro.set("Sua sessão foi encerrada. Entre novamente para continuar.");
            void this.renovarCsrf();
        } else if (e instanceof HttpErrorResponse && e.status === 403) {
            try {
                await this.sessao.identidade();
                this.erro.set("A sessão de segurança mudou. Recarregue a página e entre novamente.");
            } catch (check) {
                if (check instanceof HttpErrorResponse && check.status === 401) {
                    this.limparSessao();
                    this.erro.set("Sua sessão foi encerrada. Entre novamente para continuar.");
                    void this.renovarCsrf();
                } else {
                    this.erro.set("Não foi possível verificar sua sessão. Confira a conexão e tente novamente.");
                }
            }
        } else if (e instanceof HttpErrorResponse && e.status === 404) {
            this.erro.set("Empresa indisponível para sua conta.");
        } else if (e instanceof HttpErrorResponse && e.status === 409) {
            this.erro.set(
                "Os dados foram alterados em outra sessão. Copie suas alterações e recarregue antes de salvar."
            );
        } else {
            this.erro.set(
                e instanceof HttpErrorResponse && e.status === 400
                    ? e.error?.mensagem || "Revise os dados informados."
                    : "Não foi possível concluir. Confira a conexão e tente novamente."
            );
        }
    }

    @HostListener("window:beforeunload", ["$event"]) antesFechar(event: BeforeUnloadEvent): void {
        if (this.autenticado() && this.temAlteracoes()) {
            event.preventDefault();
            event.returnValue = "";
        }
    }
}
