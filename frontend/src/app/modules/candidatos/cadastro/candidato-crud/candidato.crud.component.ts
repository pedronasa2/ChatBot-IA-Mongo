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
import {Candidato} from "../../classes/model/candidato";
import {CandidatoService} from "../../services/candidato.service";
@Component({
    standalone: true,
    imports: [UiModule],
    templateUrl: "./candidato.crud.component.html",
    styleUrl: "./candidato.crud.component.scss"
})
export class CandidatoCrudComponent {
    private readonly api = inject(CandidatoService);
    private readonly rota = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly destruidor = inject(DestroyRef);
    private readonly formularios = inject(FormBuilder);
    private readonly acesso = inject(AcessoModulosService);
    readonly edicao = inject(EstadoEdicaoModulo);
    readonly modulo = "candidatos";
    readonly empresaId = signal("");
    readonly carregando = signal(false);
    readonly salvando = signal(false);
    readonly erro = signal("");
    readonly sucesso = signal("");
    readonly pagina = signal<PaginaRegistros<Candidato>>({registros: [], total: 0, pagina: 0, tamanho: 20});
    readonly selecionado = signal<Candidato | null>(null);
    readonly formularioAberto = signal(false);
    readonly filtros = this.formularios.nonNullable.group({
        busca: [""],
        incluirArquivados: [false]
    });
    readonly candidato = this.formularios.nonNullable.group({
        nomeCompleto: ["", [Validators.required, Validators.maxLength(160)]],
        email: ["", [Validators.email, Validators.maxLength(254)]],
        telefone: ["", Validators.maxLength(40)],
        cargoPretendido: ["", Validators.maxLength(160)],
        urlCurriculo: ["", [Validators.maxLength(2000), Validators.pattern(/^(https?:\/\/[^\s]+)?$/)]],
        resumoProfissional: ["", Validators.maxLength(10000)],
        observacoes: ["", Validators.maxLength(5000)],
        situacao: ["novo", Validators.required],
        arquivado: [false],
        revisao: [0]
    });

    constructor() {
        for (const formulario of [this.candidato]) {
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
        this.candidato.reset();
        this.formularioAberto.set(true);
        this.edicao.definir(false);
        this.erro.set("");
        this.sucesso.set("");
    }

    async editar(registro: Candidato): Promise<void> {
        if (!this.descartarEdicao()) return;
        this.erro.set("");
        const empresaId = this.empresaId();
        const geracao = ++this.geracaoEdicao;
        try {
            const atual = await firstValueFrom(this.api.consultar(empresaId, registro.id));
            if (this.empresaId() !== empresaId || geracao !== this.geracaoEdicao) return;
            this.selecionado.set(atual);
            this.candidato.reset(atual);
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
        const formulario = this.candidato;
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
            const dados = this.candidato.getRawValue();
            if (this.empresaId() !== empresaId) return;
            await firstValueFrom(this.api.salvar(empresaId, registro?.id, dados));
            if (this.empresaId() !== empresaId) return;
            this.formularioAberto.set(false);
            this.edicao.definir(false);
            this.sucesso.set("Candidato salvo.");
            await this.carregar();
        } catch (erro) {
            if (this.empresaId() === empresaId) this.tratarErro(erro);
        } finally {
            formulario.enable({emitEvent: false});
            this.salvando.set(false);
        }
    }

    async arquivar(registro: Candidato): Promise<void> {
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

    enderecoCurriculo(registro: Candidato): string {
        return this.api.enderecoCurriculo(this.empresaId(), registro.id);
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
}
