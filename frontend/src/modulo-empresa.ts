import { Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ModuloEmpresa } from './modelos-empresa';
import { ServicoAcessoModulos } from './acesso-modulos';
import { EstadoEdicaoModulo } from './estado-edicao-modulo';

interface RegistroModulo {
  id: string;
  revisao: number;
  arquivado: boolean;
  situacao: string;
  email: string;
  telefone: string;
  observacoes: string;
  nomeCompleto?: string;
  cargoPretendido?: string;
  urlCurriculo?: string;
  resumoProfissional?: string;
  recebidoEm?: string;
  nomePessoa?: string;
  assunto?: string;
  responsavel?: string;
  inicio?: string;
  fim?: string;
}
interface PaginaRegistros { registros: RegistroModulo[]; total: number; pagina: number; tamanho: number }

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule],
  templateUrl: './modulo-empresa.html',
})
export class ComponenteModuloEmpresa {
  private readonly http = inject(HttpClient);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destruidor = inject(DestroyRef);
  private readonly formularios = inject(FormBuilder);
  private readonly acesso = inject(ServicoAcessoModulos);
  readonly edicao = inject(EstadoEdicaoModulo);
  readonly modulo = this.rota.snapshot.data['modulo'] as ModuloEmpresa;
  readonly empresaId = signal('');
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly pagina = signal<PaginaRegistros>({ registros: [], total: 0, pagina: 0, tamanho: 20 });
  readonly selecionado = signal<RegistroModulo | null>(null);
  readonly formularioAberto = signal(false);
  readonly fusoHorario = Intl.DateTimeFormat().resolvedOptions().timeZone;
  readonly filtros = this.formularios.nonNullable.group({ busca: [''], incluirArquivados: [false], de: [''], ate: [''] });
  readonly candidato = this.formularios.nonNullable.group({
    nomeCompleto: ['', [Validators.required, Validators.maxLength(160)]], email: ['', [Validators.email, Validators.maxLength(254)]],
    telefone: ['', Validators.maxLength(40)], cargoPretendido: ['', Validators.maxLength(160)],
    urlCurriculo: ['', [Validators.maxLength(2000), Validators.pattern(/^(https?:\/\/[^\s]+)?$/)]],
    resumoProfissional: ['', Validators.maxLength(10000)], observacoes: ['', Validators.maxLength(5000)],
    situacao: ['novo', Validators.required], arquivado: [false], revisao: [0],
  });
  readonly agendamento = this.formularios.nonNullable.group({
    nomePessoa: ['', [Validators.required, Validators.maxLength(160)]], email: ['', [Validators.email, Validators.maxLength(254)]],
    telefone: ['', Validators.maxLength(40)], assunto: ['', [Validators.required, Validators.maxLength(200)]],
    responsavel: ['', Validators.maxLength(160)], inicio: ['', Validators.required], fim: ['', Validators.required],
    observacoes: ['', Validators.maxLength(5000)], situacao: ['agendado', Validators.required], arquivado: [false], revisao: [0],
  });

  constructor() {
    for (const formulario of [this.candidato, this.agendamento] as FormGroup[]) {
      formulario.valueChanges.pipe(takeUntilDestroyed(this.destruidor)).subscribe(() => this.edicao.pendente.set(formulario.dirty));
    }
    this.rota.paramMap.pipe(takeUntilDestroyed(this.destruidor)).subscribe(parametros => {
      this.empresaId.set(parametros.get('empresaId') || '');
      this.formularioAberto.set(false);
      this.selecionado.set(null);
      this.edicao.pendente.set(false);
      this.pagina.set({ registros: [], total: 0, pagina: 0, tamanho: 20 });
      this.filtros.reset();
      void this.carregar();
    });
    this.destruidor.onDestroy(() => this.edicao.pendente.set(false));
  }

  private geracaoCarregamento = 0;
  private endereco() { return `/api/empresas/${this.empresaId()}/${this.modulo}`; }

  async carregar(numeroPagina = 0) {
    const empresaId = this.empresaId();
    const geracao = ++this.geracaoCarregamento;
    this.carregando.set(true);
    this.erro.set('');
    try {
      const filtros = this.filtros.getRawValue();
      let parametros = new HttpParams().set('busca', filtros.busca).set('incluirArquivados', filtros.incluirArquivados)
        .set('pagina', numeroPagina).set('tamanho', 20);
      if (this.modulo === 'agenda') {
        if (filtros.de) parametros = parametros.set('de', new Date(filtros.de).toISOString());
        if (filtros.ate) parametros = parametros.set('ate', new Date(filtros.ate).toISOString());
      }
      const pagina = await firstValueFrom(this.http.get<PaginaRegistros>(this.endereco(), { params: parametros }));
      if (this.empresaId() === empresaId && geracao === this.geracaoCarregamento) this.pagina.set(pagina);
    } catch (erro) {
      if (this.empresaId() === empresaId && geracao === this.geracaoCarregamento) this.tratarErro(erro);
    } finally {
      if (this.empresaId() === empresaId && geracao === this.geracaoCarregamento) this.carregando.set(false);
    }
  }

  novo() {
    if (!this.descartarEdicao()) return;
    this.selecionado.set(null);
    this.candidato.reset();
    this.agendamento.reset();
    this.formularioAberto.set(true);
    this.edicao.pendente.set(false);
    this.erro.set('');
    this.sucesso.set('');
  }

  async editar(registro: RegistroModulo) {
    if (!this.descartarEdicao()) return;
    this.erro.set('');
    const empresaId = this.empresaId();
    try {
      const atual = await firstValueFrom(this.http.get<RegistroModulo>(`${this.endereco()}/${registro.id}`));
      if (this.empresaId() !== empresaId) return;
      this.selecionado.set(atual);
      if (this.modulo === 'candidatos') this.candidato.reset(atual);
      else this.agendamento.reset({ ...atual, inicio: this.horarioLocal(atual.inicio!), fim: this.horarioLocal(atual.fim!) });
      this.formularioAberto.set(true);
      this.edicao.pendente.set(false);
    } catch (erro) { if (this.empresaId() === empresaId) this.tratarErro(erro); }
  }

  fechar() {
    if (!this.descartarEdicao()) return;
    this.formularioAberto.set(false);
    this.edicao.pendente.set(false);
  }

  private descartarEdicao() {
    return !this.edicao.pendente() || window.confirm('Há alterações não salvas neste registro. Deseja descartá-las?');
  }

  async salvar() {
    const formulario = this.modulo === 'candidatos' ? this.candidato : this.agendamento;
    if (formulario.invalid) {
      formulario.markAllAsTouched();
      this.erro.set('Revise os campos destacados antes de salvar.');
      return;
    }
    if (this.salvando()) return;
    const empresaId = this.empresaId();
    const endereco = this.endereco();
    const registro = this.selecionado();
    formulario.disable({ emitEvent: false });
    this.salvando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    try {
      const dados = this.modulo === 'candidatos' ? this.candidato.getRawValue() : {
        ...this.agendamento.getRawValue(),
        inicio: new Date(this.agendamento.controls.inicio.value).toISOString(),
        fim: new Date(this.agendamento.controls.fim.value).toISOString(),
      };
      const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
      const opcoes = { headers: new HttpHeaders({ [csrf.headerName]: csrf.token }) };
      if (this.empresaId() !== empresaId) return;
      await firstValueFrom(registro
        ? this.http.put<RegistroModulo>(`${endereco}/${registro.id}`, dados, opcoes)
        : this.http.post<RegistroModulo>(endereco, dados, opcoes));
      if (this.empresaId() !== empresaId) return;
      this.formularioAberto.set(false);
      this.edicao.pendente.set(false);
      this.sucesso.set(this.modulo === 'candidatos' ? 'Candidato salvo.' : 'Agendamento salvo.');
      await this.carregar();
    } catch (erro) { if (this.empresaId() === empresaId) this.tratarErro(erro); }
    finally { formulario.enable({ emitEvent: false }); this.salvando.set(false); }
  }

  async arquivar(registro: RegistroModulo) {
    if (this.salvando() || !window.confirm('Arquivar este registro? Ele poderá ser restaurado depois.')) return;
    const empresaId = this.empresaId();
    const endereco = this.endereco();
    this.salvando.set(true);
    this.erro.set('');
    try {
      const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
      if (this.empresaId() !== empresaId) return;
      await firstValueFrom(this.http.delete(`${endereco}/${registro.id}`, {
        headers: new HttpHeaders({ [csrf.headerName]: csrf.token }), params: { revisao: registro.revisao },
      }));
      if (this.empresaId() !== empresaId) return;
      if (this.selecionado()?.id === registro.id) { this.formularioAberto.set(false); this.edicao.pendente.set(false); }
      this.sucesso.set('Registro arquivado.');
      await this.carregar();
    } catch (erro) { if (this.empresaId() === empresaId) this.tratarErro(erro); }
    finally { this.salvando.set(false); }
  }

  private tratarErro(erro: unknown) {
    const bloqueado = erro instanceof HttpErrorResponse && (erro.status === 401 || erro.status === 404
      || (erro.status === 403 && /desabilitad/.test(erro.error?.mensagem || '')));
    if (bloqueado) {
      if (erro instanceof HttpErrorResponse && erro.status === 401) this.acesso.sessaoEncerrada.set(true);
      this.formularioAberto.set(false);
      this.pagina.set({ registros: [], total: 0, pagina: 0, tamanho: 20 });
      this.edicao.pendente.set(false);
      this.acesso.negacao.set(erro instanceof HttpErrorResponse && erro.error?.mensagem
        ? erro.error.mensagem : 'Acesso ao módulo indisponível.');
      void this.router.navigateByUrl('/');
      return;
    }
    this.erro.set(erro instanceof HttpErrorResponse && erro.error?.mensagem
      ? erro.error.mensagem : 'Não foi possível concluir a operação. Tente novamente.');
  }

  enderecoCurriculo(registro: RegistroModulo) { return `${this.endereco()}/${registro.id}/curriculo`; }
  data(valor?: string) { return valor ? new Date(valor).toLocaleString('pt-BR') : '—'; }
  situacao(valor: string) {
    return ({ novo: 'Novo', em_analise: 'Em análise', aprovado: 'Aprovado', reprovado: 'Reprovado', agendado: 'Agendado',
      confirmado: 'Confirmado', concluido: 'Concluído', cancelado: 'Cancelado' } as Record<string, string>)[valor] || valor;
  }
  private horarioLocal(valor: string) {
    const data = new Date(valor);
    const local = new Date(data.getTime() - data.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
  }
}
