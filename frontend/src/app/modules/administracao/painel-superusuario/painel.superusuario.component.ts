import {HttpErrorResponse} from "@angular/common/http";
import {Component, DestroyRef, EventEmitter, Output, inject, signal} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormBuilder, Validators} from "@angular/forms";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../ui/ui.module";
import {MetricasAtendimentoService} from "../services/metricas.atendimento.service";

import {PromptGlobal} from "../classes/model/prompt.global";
import {PreviaPrompt} from "../classes/vo/previa.prompt";
import {ResumoMetricas} from "../classes/vo/resumo.metricas";
import {VersaoPromptGlobal} from "../classes/vo/versao.prompt.global";
import {PromptGlobalService} from "../services/prompt.global.service";
@Component({
    selector: "painel-superusuario",
    standalone: true,
    imports: [UiModule],
    templateUrl: "./painel.superusuario.component.html",
    styleUrl: "./painel.superusuario.component.scss"
})
export class PainelSuperusuarioComponent {
    private readonly api = inject(PromptGlobalService);
    private readonly metricasService = inject(MetricasAtendimentoService);
    private readonly destruidor = inject(DestroyRef);
    private readonly formularios = inject(FormBuilder);
    @Output() alteracoesPendentes = new EventEmitter<boolean>();
    @Output() sessaoIndisponivel = new EventEmitter<void>();
    readonly secao = signal("prompt");
    readonly carregando = signal(true);
    readonly salvando = signal(false);
    readonly atualizandoMetricas = signal(false);
    readonly carregandoPrevia = signal(false);
    readonly erro = signal("");
    readonly sucesso = signal("");
    readonly promptSalvo = signal<PromptGlobal | null>(null);
    readonly historico = signal<VersaoPromptGlobal[]>([]);
    readonly metricas = signal<ResumoMetricas | null>(null);
    readonly empresaPrevia = signal("");
    readonly previa = signal<PreviaPrompt | null>(null);
    readonly formularioPrompt = this.formularios.nonNullable.group({
        promptSistema: ["", [Validators.required, Validators.maxLength(40000)]],
        complementarConfiguracoesEmpresa: [true],
        revisao: [0]
    });

    constructor() {
        this.formularioPrompt.valueChanges
            .pipe(takeUntilDestroyed(this.destruidor))
            .subscribe(() => this.alteracoesPendentes.emit(this.formularioPrompt.dirty));
        void this.carregar();
        const intervalo = setInterval(() => {
            if (this.secao() === "metricas" && !document.hidden) void this.atualizarMetricas();
        }, 30000);
        this.destruidor.onDestroy(() => clearInterval(intervalo));
    }

    async carregar(): Promise<void> {
        this.carregando.set(true);
        this.erro.set("");
        try {
            const [prompt, historico, metricas] = await Promise.all([
                firstValueFrom(this.api.prompt()),
                firstValueFrom(this.api.historico()),
                firstValueFrom(this.metricasService.consultar())
            ]);
            this.promptSalvo.set(prompt);
            this.formularioPrompt.reset(prompt);
            this.alteracoesPendentes.emit(false);
            this.historico.set(historico);
            this.metricas.set(metricas);
            this.empresaPrevia.set(metricas.porEmpresa[0]?.empresaId || "");
        } catch (erro) {
            this.tratarErro(erro);
        } finally {
            this.carregando.set(false);
        }
    }

    async salvarPrompt(): Promise<void> {
        if (this.formularioPrompt.invalid || this.salvando()) {
            this.formularioPrompt.markAllAsTouched();
            return;
        }
        this.formularioPrompt.disable({emitEvent: false});
        this.salvando.set(true);
        this.erro.set("");
        this.sucesso.set("");
        try {
            const prompt = await firstValueFrom(this.api.salvar(this.formularioPrompt.getRawValue()));
            this.promptSalvo.set(prompt);
            this.formularioPrompt.reset(prompt);
            this.alteracoesPendentes.emit(false);
            this.previa.set(null);
            // A confirmação de gravação independe da atualização da lista de versões.
            this.historico.update((versoes) =>
                [
                    {
                        promptSistema: prompt.promptSistema,
                        complementarConfiguracoesEmpresa: prompt.complementarConfiguracoesEmpresa,
                        revisao: prompt.revisao,
                        criadoEm: prompt.atualizadoEm,
                        usuarioResponsavelEmail: prompt.atualizadoPorEmail || ""
                    },
                    ...versoes
                ].slice(0, 20)
            );
            this.sucesso.set("Prompt global salvo. Nova versão registrada no histórico.");
        } catch (erro) {
            this.tratarErro(erro);
        } finally {
            this.formularioPrompt.enable({emitEvent: false});
            this.salvando.set(false);
        }
    }

    async atualizarMetricas(): Promise<void> {
        if (this.atualizandoMetricas()) return;
        this.atualizandoMetricas.set(true);
        this.erro.set("");
        try {
            this.metricas.set(await firstValueFrom(this.metricasService.consultar()));
        } catch (erro) {
            this.tratarErro(erro);
        } finally {
            this.atualizandoMetricas.set(false);
        }
    }

    private geracaoPrevia = 0;
    selecionarPrevia(id: string): void {
        ++this.geracaoPrevia;
        this.empresaPrevia.set(id);
        this.previa.set(null);
        this.carregandoPrevia.set(false);
    }
    async visualizarPrompt(): Promise<void> {
        if (!this.empresaPrevia() || this.formularioPrompt.dirty || this.carregandoPrevia()) return;
        const id = this.empresaPrevia();
        const geracao = ++this.geracaoPrevia;
        this.carregandoPrevia.set(true);
        this.erro.set("");
        try {
            const previa = await firstValueFrom(this.api.previa(id));
            if (geracao === this.geracaoPrevia && id === this.empresaPrevia()) this.previa.set(previa);
        } catch (erro) {
            if (geracao === this.geracaoPrevia) this.tratarErro(erro);
        } finally {
            if (geracao === this.geracaoPrevia) this.carregandoPrevia.set(false);
        }
    }

    data(valor: string | null): string {
        return valor ? new Date(valor).toLocaleString("pt-BR") : "Nenhuma mensagem registrada";
    }

    hora(valor: string): string {
        return new Date(valor).toLocaleTimeString("pt-BR", {hour: "2-digit", minute: "2-digit"});
    }

    numero(valor: number): string {
        return valor.toLocaleString("pt-BR");
    }

    alturaBarra(mensagens: number): number {
        const maximo = Math.max(1, ...(this.metricas()?.volumePorMinuto.map((volume) => volume.mensagens) || []));
        return (mensagens / maximo) * 100;
    }

    private tratarErro(erro: unknown): void {
        if (
            erro instanceof HttpErrorResponse &&
            (erro.status === 401 ||
                (erro.status === 403 && erro.error?.mensagem === "Acesso exclusivo de superusuário."))
        ) {
            this.sessaoIndisponivel.emit();
            return;
        }
        if (erro instanceof HttpErrorResponse && erro.status === 403) {
            this.erro.set("A sessão de segurança mudou. Tente salvar novamente; suas alterações foram mantidas.");
            return;
        }
        this.erro.set(
            erro instanceof HttpErrorResponse && erro.error?.mensagem
                ? erro.error.mensagem
                : "Não foi possível carregar ou salvar. Tente novamente."
        );
    }
}
