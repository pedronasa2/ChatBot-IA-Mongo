import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient } from "@angular/common/http";
import { ComponenteAplicacao } from "./app";
import { provideRouter } from '@angular/router';
import { rotas } from './rotas';
bootstrapApplication(ComponenteAplicacao, { providers: [provideHttpClient(), provideRouter(rotas)] }).catch(
  () => console.error("Não foi possível iniciar o painel."),
);
