import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {RestService} from "../../core/http/rest.service";
import {PromptGlobal} from "../classes/model/prompt.global";
import {DadosPromptGlobal} from "../classes/vo/dados.prompt.global";
import {PreviaPrompt} from "../classes/vo/previa.prompt";
import {VersaoPromptGlobal} from "../classes/vo/versao.prompt.global";

@Injectable({providedIn: "root"})
export class PromptGlobalService {
    constructor(private readonly restService: RestService) {}

    prompt(): Observable<PromptGlobal> {
        return this.restService.get("/api/superusuario/prompt-global", PromptGlobal);
    }
    historico(): Observable<VersaoPromptGlobal[]> {
        return this.restService.list("/api/superusuario/prompt-global/historico", VersaoPromptGlobal);
    }
    salvar(dados: DadosPromptGlobal): Observable<PromptGlobal> {
        return this.restService.put("/api/superusuario/prompt-global", dados, PromptGlobal);
    }
    previa(empresaId: string): Observable<PreviaPrompt> {
        return this.restService.get(`/api/superusuario/prompt-global/previa/${empresaId}`, PreviaPrompt);
    }
}
