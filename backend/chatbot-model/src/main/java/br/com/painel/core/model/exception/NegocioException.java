package br.com.painel.core.model.exception;

public class NegocioException extends RuntimeException {
    private final ErroNegocio erro;

    public NegocioException(ErroNegocio erro, String mensagem) {
        super(mensagem);
        this.erro = erro;
    }

    public ErroNegocio getErro() {
        return erro;
    }
}
