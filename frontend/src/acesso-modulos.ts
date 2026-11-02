import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Empresa, ModuloEmpresa } from './modelos-empresa';

@Injectable({ providedIn: 'root' })
export class ServicoAcessoModulos {
  readonly empresaVerificada = signal<Empresa | null>(null);
  readonly sessaoEncerrada = signal(false);
  readonly negacao = signal('');
  private readonly http = inject(HttpClient);

  async verificar(empresaId: string, modulo: ModuloEmpresa) {
    this.negacao.set('');
    try {
      const empresa = await firstValueFrom(this.http.get<Empresa>(`/api/empresas/${empresaId}`));
      this.empresaVerificada.set(empresa);
      if (modulo === 'candidatos' ? empresa.moduloCandidatosHabilitado : empresa.moduloAgendaHabilitado) return true;
      this.negacao.set(modulo === 'candidatos'
        ? 'Banco de candidatos desabilitado para esta empresa.'
        : 'Agenda de atendimentos desabilitada para esta empresa.');
    } catch (erro) {
      if (erro instanceof HttpErrorResponse && erro.status === 401) this.sessaoEncerrada.set(true);
      if (!(erro instanceof HttpErrorResponse && erro.status === 401)) {
        this.negacao.set('Não foi possível acessar o módulo desta empresa.');
      }
    }
    return false;
  }
}

export const protegerModuloEmpresa: CanActivateFn = async rota => {
  const acesso = inject(ServicoAcessoModulos);
  const router = inject(Router);
  const empresaId = rota.paramMap.get('empresaId') || '';
  return await acesso.verificar(empresaId, rota.data['modulo']) ? true : router.createUrlTree(['/']);
};
