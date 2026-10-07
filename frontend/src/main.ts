import { bootstrapApplication } from "@angular/platform-browser";
import { provideHttpClient } from "@angular/common/http";
import { ComponenteAplicacao } from "./app";
bootstrapApplication(ComponenteAplicacao, { providers: [provideHttpClient()] }).catch(
  () => console.error("Não foi possível iniciar o painel."),
);
