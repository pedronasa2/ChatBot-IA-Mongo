export interface Empresa {
  id: string;
  nome: string;
  descricao: string;
  endereco: string;
  contato: string;
  site: string;
  servicos: string;
  fusoHorario: string;
  revisao: number;
  botAtivo: boolean;
  moduloCandidatosHabilitado: boolean;
  moduloAgendaHabilitado: boolean;
}
export type ModuloEmpresa = 'candidatos' | 'agenda';
