package br.com.painel.candidatos.service;

import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;

import java.net.URI;
import java.util.Set;

public final class CandidatoValidator {
    private CandidatoValidator() {}

    public static void urlCurriculo(String endereco) {
        if (endereco.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(endereco);
            if (!Set.of("http", "https").contains(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException erro) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS,
                    "Informe um endereço de currículo com http:// ou https://.");
        }
    }
}
