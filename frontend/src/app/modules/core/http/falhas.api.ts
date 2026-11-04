import {HttpErrorResponse} from "@angular/common/http";
export function mensagemFalha(
    erro: unknown,
    padrao = "Não foi possível concluir a operação. Tente novamente."
): string {
    return erro instanceof HttpErrorResponse && erro.error?.mensagem ? erro.error.mensagem : padrao;
}
