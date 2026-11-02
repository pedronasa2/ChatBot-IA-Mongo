import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { protegerModuloEmpresa } from './acesso-modulos';

@Component({ standalone: true, template: '' })
class PaginaInicialVazia {}

export const rotas: Routes = [
  { path: '', pathMatch: 'full', component: PaginaInicialVazia },
  {
    path: 'empresas/:empresaId/candidatos',
    loadComponent: () => import('./modulo-empresa').then(modulo => modulo.ComponenteModuloEmpresa),
    canActivate: [protegerModuloEmpresa], data: { modulo: 'candidatos' },
  },
  {
    path: 'empresas/:empresaId/agenda',
    loadComponent: () => import('./modulo-empresa').then(modulo => modulo.ComponenteModuloEmpresa),
    canActivate: [protegerModuloEmpresa], data: { modulo: 'agenda' },
  },
  { path: '**', redirectTo: '' },
];
