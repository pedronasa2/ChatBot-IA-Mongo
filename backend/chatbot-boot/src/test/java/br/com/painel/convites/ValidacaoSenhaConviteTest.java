package br.com.painel.convites;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.painel.convites.model.vo.AtivacaoConvite;

import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

class ValidacaoSenhaConviteTest {
    @Test
    void aceitaLimitesERejeitaSenhasForaDoIntervalo() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (int tamanho : new int[] {5, 6, 21, 22}) {
                String senha = "a".repeat(tamanho);
                var dados = new AtivacaoConvite("token", "cliente@example.test", senha, senha);
                var erros = validator.validate(dados);
                if (tamanho == 6 || tamanho == 21) {
                    assertThat(erros).isEmpty();
                } else {
                    assertThat(erros)
                            .extracting(e -> e.getPropertyPath().toString())
                            .containsExactlyInAnyOrder("senha", "confirmacaoSenha");
                }
            }
        }
    }
}
