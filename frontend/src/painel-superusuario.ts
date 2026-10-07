import { Component, DestroyRef, EventEmitter, Output, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { UiIconComponent } from './ui-icon';

interface PromptGlobal {
  promptSistema: string;
  complementarConfiguracoesEmpresa: boolean;
  revisao: number;
  atualizadoEm: string;
  atualizadoPorEmail: string | null;
}
interface VersaoPromptGlobal {
  promptSistema: string;
  complementarConfiguracoesEmpresa: boolean;
  revisao: number;
  criadoEm: string;
  usuarioResponsavelEmail: string;
}
interface ResumoMetricas {
  mensagensUltimoMinuto: number;
  totalMensagens: number;
  mensagensUltimas24Horas: number;
  mensagensRecebidas: number;
  mensagensEnviadas: number;
  errosUltimas24Horas: number;
  totalEmpresas: number;
  totalUsuariosAtivos: number;
  ultimaMensagem: string | null;
  atualizadoEm: string;
  volumePorMinuto: { minuto: string; mensagens: number }[];
  porEmpresa: { empresaId: string; nomeEmpresa: string; totalMensagens: number; mensagensUltimas24Horas: number }[];
}

@Component({
  selector: 'painel-superusuario',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, UiIconComponent],
  templateUrl: './painel-superusuario.html',
})
export class ComponentePainelSuperusuario {
  private readonly http = inject(HttpClient);
  private readonly destruidor = inject(DestroyRef);
  private readonly formularios = inject(FormBuilder);
  @Output() alteracoesPendentes = new EventEmitter<boolean>();
  @Output() sessaoIndisponivel = new EventEmitter<void>();
  readonly secao = signal('prompt');
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly atualizandoMetricas = signal(false);
  readonly carregandoPrevia = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly promptSalvo = signal<PromptGlobal | null>(null);
  readonly historico = signal<VersaoPromptGlobal[]>([]);
  readonly metricas = signal<ResumoMetricas | null>(null);
  readonly empresaPrevia = signal('');
  readonly previa = signal<{ nomeEmpresa: string; promptComposto: string } | null>(null);
  readonly formularioPrompt = this.formularios.nonNullable.group({
    promptSistema: ['', [Validators.required, Validators.maxLength(40000)]],
    complementarConfiguracoesEmpresa: [true],
    revisao: [0],
  });

  constructor() {
    this.formularioPrompt.valueChanges.pipe(takeUntilDestroyed(this.destruidor))
      .subscribe(() => this.alteracoesPendentes.emit(this.formularioPrompt.dirty));
    void this.carregar();
    const intervalo = setInterval(() => {
      if (this.secao() === 'metricas' && !document.hidden) void this.atualizarMetricas();
    }, 30000);
    this.destruidor.onDestroy(() => clearInterval(intervalo));
  }

  async carregar() {
    this.carregando.set(true);
    this.erro.set('');
    try {
      const [prompt, historico, metricas] = await Promise.all([
        firstValueFrom(this.http.get<PromptGlobal>('/api/superusuario/prompt-global')),
        firstValueFrom(this.http.get<VersaoPromptGlobal[]>('/api/superusuario/prompt-global/historico')),
        firstValueFrom(this.http.get<ResumoMetricas>('/api/superusuario/metricas')),
      ]);
      this.promptSalvo.set(prompt);
      this.formularioPrompt.reset(prompt);
      this.alteracoesPendentes.emit(false);
      this.historico.set(historico);
      this.metricas.set(metricas);
      this.empresaPrevia.set(metricas.porEmpresa[0]?.empresaId || '');
    } catch (erro) {
      this.tratarErro(erro);
    } finally {
      this.carregando.set(false);
    }
  }

  async salvarPrompt() {
    if (this.formularioPrompt.invalid || this.salvando()) {
      this.formularioPrompt.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    try {
      const csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/auth/csrf'));
      const prompt = await firstValueFrom(this.http.put<PromptGlobal>(
        '/api/superusuario/prompt-global', this.formularioPrompt.getRawValue(),
        { headers: new HttpHeaders({ [csrf.headerName]: csrf.token }) },
      ));
      this.promptSalvo.set(prompt);
      this.formularioPrompt.reset(prompt);
      this.alteracoesPendentes.emit(false);
      this.previa.set(null);
      // A confirmação de gravação independe da atualização da lista de versões.
      this.historico.update(versoes => [{
        promptSistema: prompt.promptSistema,
        complementarConfiguracoesEmpresa: prompt.complementarConfiguracoesEmpresa,
        revisao: prompt.revisao,
        criadoEm: prompt.atualizadoEm,
        usuarioResponsavelEmail: prompt.atualizadoPorEmail || '',
      }, ...versoes].slice(0, 20));
      this.sucesso.set('Prompt global salvo. Nova versão registrada no histórico.');
    } catch (erro) {
      this.tratarErro(erro);
    } finally {
      this.salvando.set(false);
    }
  }

  async atualizarMetricas() {
    if (this.atualizandoMetricas()) return;
    this.atualizandoMetricas.set(true);
    this.erro.set('');
    try {
      this.metricas.set(await firstValueFrom(this.http.get<ResumoMetricas>('/api/superusuario/metricas')));
    } catch (erro) {
      this.tratarErro(erro);
    } finally {
      this.atualizandoMetricas.set(false);
    }
  }

  async visualizarPrompt() {
    if (!this.empresaPrevia() || this.formularioPrompt.dirty || this.carregandoPrevia()) return;
    this.carregandoPrevia.set(true);
    this.erro.set('');
    try {
      this.previa.set(await firstValueFrom(this.http.get<{ nomeEmpresa: string; promptComposto: string }>(
        `/api/superusuario/prompt-global/previa/${this.empresaPrevia()}`,
      )));
    } catch (erro) {
      this.tratarErro(erro);
    } finally {
      this.carregandoPrevia.set(false);
    }
  }

  data(valor: string | null) {
    return valor ? new Date(valor).toLocaleString('pt-BR') : 'Nenhuma mensagem registrada';
  }

  hora(valor: string) {
    return new Date(valor).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }

  numero(valor: number) {
    return valor.toLocaleString('pt-BR');
  }

  alturaBarra(mensagens: number) {
    const maximo = Math.max(1, ...(this.metricas()?.volumePorMinuto.map(volume => volume.mensagens) || []));
    return mensagens / maximo * 100;
  }

  private tratarErro(erro: unknown) {
    if (erro instanceof HttpErrorResponse && (erro.status === 401
        || (erro.status === 403 && erro.error?.mensagem === 'Acesso exclusivo de superusuário.'))) {
      this.sessaoIndisponivel.emit();
      return;
    }
    if (erro instanceof HttpErrorResponse && erro.status === 403) {
      this.erro.set('A sessão de segurança mudou. Tente salvar novamente; suas alterações foram mantidas.');
      return;
    }
    this.erro.set(erro instanceof HttpErrorResponse && erro.error?.mensagem
      ? erro.error.mensagem : 'Não foi possível carregar ou salvar. Tente novamente.');
  }
}
