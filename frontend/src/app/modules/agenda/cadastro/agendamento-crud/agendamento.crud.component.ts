import {HttpErrorResponse} from "@angular/common/http";
import {Component, DestroyRef, inject, signal} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormBuilder, Validators} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {firstValueFrom} from "rxjs";
import {UiModule} from "../../../ui/ui.module";

import {AcessoModulosService} from "../../../core/security/acesso.modulos.service";
import {EstadoEdicaoModulo} from "../../../core/security/estado.edicao.modulo.service";

import {PaginaRegistros} from "../../../core/classes/vo/pagina.registros";
import {acessoModuloIndisponivel} from "../../../core/http/falhas.api";
import {Agendamento} from "../../classes/model/agendamento";
import {AgendamentoService} from "../../services/agendamento.service";

@Component({
    standalone: true,
    imports: [UiModule],
    templateUrl: "./agendamento.crud.component.html",
    styleUrl: "./agendamento.crud.component.scss"
})
export class AgendamentoCrudComponent {
    private readonly api = inject(AgendamentoService);
    private readonly rota = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly destruidor = inject(DestroyRef);
    private readonly formularios = inject(FormBuilder);
    private readonly acesso = inject(AcessoModulosService);
    readonly edicao = inject(EstadoEdicaoModulo);
    readonly modulo = "agenda";
    readonly empresaId = signal("");
    readonly carregando = signal(false);
    readonly salvando = signal(false);
    readonly erro = signal("");
    readonly sucesso = signal("");
    readonly pagina = signal<PaginaRegistros<Agendamento>>({registros: [], total: 0, pagina: 0, tamanho: 20});
    readonly selecionado = signal<Agendamento | null>(null);
    readonly formularioAberto = signal(false);
    readonly fusoHorario = Intl.DateTimeFormat().resolvedOptions().timeZone;
    readonly filtros = this.formularios.nonNullable.group({
        busca: [""],
        incluirArquivados: [false],
        de: [""],
        ate: [""]
    });

    readonly agendamento = this.formularios.nonNullable.group({
        nomePessoa: ["", [Validators.required, Validators.maxLength(160)]],
        email: ["", [Validators.email, Validators.maxLength(254)]],
        telefone: ["", Validators.maxLength(40)],
        assunto: ["", [Validators.required, Validators.maxLength(200)]],
        responsavel: ["", Validators.maxLength(160)],
        inicio: ["", Validators.required],
        fim: ["", Validators.required],
        observacoes: ["", Validators.maxLength(5000)],
        situacao: ["agendado", Validators.required],
        arquivado: [false],
        revisao: [0]
    });

    constructor() {
        for (const formulario of [this.agendamento]) {
            formulario.valueChanges
                .pipe(takeUntilDestroyed(this.destruidor))
                .subscribe(() => this.edicao.definir(formulario.dirty));
        }
        this.rota.paramMap.pipe(takeUntilDestroyed(this.destruidor)).subscribe((parametros) => {
            ++this.geracaoEdicao;
            this.empresaId.set(parametros.get("empresaId") || "");
            this.formularioAberto.set(false);
            this.selecionado.set(null);
            this.edicao.definir(false);
            this.pagina.set({registros: [], total: 0, pagina: 0, tamanho: 20});
            this.filtros.reset();
            void this.carregar();
        });
        this.destruidor.onDestroy(() => this.edicao.definir(false));
    }

    private geracaoCarregamento = 0;
    private geracaoEdicao = 0;

    private endereco(): string {
        return `/api/empresas/${this.empresaId()}/${this.modulo}`;
    }

    async carregar(numeroPagina = 0): Promise<void> {
        const empresaId = this.empresaId();
        const geracao = ++this.geracaoCarregamento;
        this.carregando.set(true);
        this.erro.set("");
        try {
            const pagina = await firstValueFrom(this.api.listar(empresaId, this.filtros.getRawValue(), numeroPagina));
            if (this.empresaId() === empresaId && geracao === this.geracaoCarregamento) this.pagina.set(pagina);
        } catch (erro) {
            if (this.empresaId() === empresaId && geracao === this.geracaoCarregamento) this.tratarErro(erro);
        } finally {
            if (this.empresaId() === empresaId && geracao === this.geracaoCarregamento) this.carregando.set(false);
        }
    }

    novo(): void {
        ++this.geracaoEdicao;
        if (!this.descartarEdicao()) return;
        this.selecionado.set(null);
        this.agendamento.reset();
        this.formularioAberto.set(true);
        this.edicao.definir(false);
        this.erro.set("");
        this.sucesso.set("");
    }

    async editar(registro: Agendamento): Promise<void> {
        if (!this.descartarEdicao()) return;
        this.erro.set("");
        const empresaId = this.empresaId();
        const geracao = ++this.geracaoEdicao;
        try {
            const atual = await firstValueFrom(this.api.consultar(empresaId, registro.id));
            if (this.empresaId() !== empresaId || geracao !== this.geracaoEdicao) return;
            this.selecionado.set(atual);
            this.agendamento.reset({
                ...atual,
                inicio: this.horarioLocal(atual.inicio),
                fim: this.horarioLocal(atual.fim)
            });
            this.formularioAberto.set(true);
            this.edicao.definir(false);
        } catch (erro) {
            if (this.empresaId() === empresaId && geracao === this.geracaoEdicao) this.tratarErro(erro);
        }
    }

    fechar(): void {
        ++this.geracaoEdicao;
        if (!this.descartarEdicao()) return;
        this.formularioAberto.set(false);
        this.edicao.definir(false);
    }

    podeSair(): boolean {
        return !this.salvando() && this.descartarEdicao();
    }

    private descartarEdicao(): boolean {
        return (
            !this.edicao.pendente() || window.confirm("Há alterações não salvas neste registro. Deseja descartá-las?")
        );
    }

    async salvar(): Promise<void> {
        const formulario = this.agendamento;
        if (formulario.invalid) {
            formulario.markAllAsTouched();
            this.erro.set("Revise os campos destacados antes de salvar.");
            return;
        }
        if (this.salvando()) return;
        const empresaId = this.empresaId();
        const endereco = this.endereco();
        const registro = this.selecionado();
        formulario.disable({emitEvent: false});
        this.salvando.set(true);
        this.erro.set("");
        this.sucesso.set("");
        try {
            const dados = {
                ...this.agendamento.getRawValue(),
                inicio: new Date(this.agendamento.controls.inicio.value).toISOString(),
                fim: new Date(this.agendamento.controls.fim.value).toISOString()
            };
            if (this.empresaId() !== empresaId) return;
            await firstValueFrom(this.api.salvar(empresaId, registro?.id, dados));
            if (this.empresaId() !== empresaId) return;
            this.formularioAberto.set(false);
            this.edicao.definir(false);
            this.sucesso.set("Agendamento salvo.");
            await this.carregar();
        } catch (erro) {
            if (this.empresaId() === empresaId) this.tratarErro(erro);
        } finally {
            formulario.enable({emitEvent: false});
            this.salvando.set(false);
        }
    }

    async arquivar(registro: Agendamento): Promise<void> {
        if (this.salvando() || !window.confirm("Arquivar este registro? Ele poderá ser restaurado depois.")) return;
        const empresaId = this.empresaId();
        const endereco = this.endereco();
        this.salvando.set(true);
        this.erro.set("");
        try {
            await firstValueFrom(this.api.arquivar(empresaId, registro));
            if (this.empresaId() !== empresaId) return;
            if (this.selecionado()?.id === registro.id) {
                this.formularioAberto.set(false);
                this.edicao.definir(false);
            }
            this.sucesso.set("Registro arquivado.");
            await this.carregar();
        } catch (erro) {
            if (this.empresaId() === empresaId) this.tratarErro(erro);
        } finally {
            this.salvando.set(false);
        }
    }

    private tratarErro(erro: unknown): void {
        const bloqueado = acessoModuloIndisponivel(erro);
        if (bloqueado) {
            if (erro instanceof HttpErrorResponse && erro.status === 401) this.acesso.definirSessaoEncerrada(true);
            this.formularioAberto.set(false);
            this.pagina.set({registros: [], total: 0, pagina: 0, tamanho: 20});
            this.edicao.definir(false);
            this.acesso.definirNegacao(
                erro instanceof HttpErrorResponse && erro.error?.mensagem
                    ? erro.error.mensagem
                    : "Acesso ao módulo indisponível."
            );
            void this.router.navigateByUrl("/");
            return;
        }
        this.erro.set(
            erro instanceof HttpErrorResponse && erro.error?.mensagem
                ? erro.error.mensagem
                : "Não foi possível concluir a operação. Tente novamente."
        );
    }

    data(valor?: string): string {
        return valor ? new Date(valor).toLocaleString("pt-BR") : "—";
    }

    situacao(valor: string): string {
        return (
            (
                {
                    novo: "Novo",
                    em_analise: "Em análise",
                    aprovado: "Aprovado",
                    reprovado: "Reprovado",
                    agendado: "Agendado",
                    confirmado: "Confirmado",
                    concluido: "Concluído",
                    cancelado: "Cancelado"
                } as Record<string, string>
            )[valor] || valor
        );
    }

    private horarioLocal(valor: string): string {
        const data = new Date(valor);
        const local = new Date(data.getTime() - data.getTimezoneOffset() * 60000);
        return local.toISOString().slice(0, 16);
    }
}
