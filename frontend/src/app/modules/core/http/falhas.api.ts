import {HttpErrorResponse} from "@angular/common/http";
export function mensagemFalha(
    erro: unknown,
    padrao = "Não foi possível concluir a operação. Tente novamente."
): string {
    return erro instanceof HttpErrorResponse && erro.error?.mensagem ? erro.error.mensagem : padrao;
}
export function acessoModuloIndisponivel(erro: unknown): boolean {
    return (
        erro instanceof HttpErrorResponse &&
        (erro.status === 401 ||
            erro.status === 404 ||
            (erro.status === 403 && /desabilitad/.test(erro.error?.mensagem || "")))
    );
}
