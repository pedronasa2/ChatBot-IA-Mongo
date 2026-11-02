import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class EstadoEdicaoModulo {
  readonly pendente = signal(false);
}
