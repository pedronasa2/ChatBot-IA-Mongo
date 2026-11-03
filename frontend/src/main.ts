import {provideHttpClient} from "@angular/common/http";
import {bootstrapApplication} from "@angular/platform-browser";
import {provideRouter, withRouterConfig} from "@angular/router";
import {rotas} from "./app/app.routes";
import {AppComponent} from "./app/modules/app/app.component";
bootstrapApplication(AppComponent, {
    providers: [provideHttpClient(), provideRouter(rotas, withRouterConfig({canceledNavigationResolution: "computed"}))]
}).catch(() => console.error("Não foi possível iniciar o painel."));
