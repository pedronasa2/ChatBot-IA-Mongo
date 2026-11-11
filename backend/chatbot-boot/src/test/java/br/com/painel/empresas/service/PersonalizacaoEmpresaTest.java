package br.com.painel.empresas.service;

import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.service.security.AcessoEmpresa;
import br.com.painel.core.service.security.AcessoSuperusuario;
import br.com.painel.empresas.model.Empresa;
import br.com.painel.empresas.model.vo.DadosEmpresa;
import br.com.painel.empresas.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalizacaoEmpresaTest {
    @Test void validaConteudoRealDaFoto() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, 2), "png", bytes);
        String foto = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
        assertThatCode(() -> EmpresaService.validarFoto(foto)).doesNotThrowAnyException();
        assertThatCode(() -> EmpresaService.validarFoto("")).doesNotThrowAnyException();
        for (String invalida : List.of("https://site/foto.png", "data:image/svg+xml;base64,PHN2Zz4=",
                "data:image/png;base64,dGV4dG8=", foto.replace("image/png", "image/jpeg"))) {
            assertThatThrownBy(() -> EmpresaService.validarFoto(invalida)).isInstanceOf(NegocioException.class);
        }
    }
    @Test void salvaIdentidadeNaEmpresaAutorizada() {
        var acesso = mock(AcessoEmpresa.class);
        var repo = mock(EmpresaRepository.class);
        var service = new EmpresaService(acesso, repo, mock(AcessoSuperusuario.class));
        var empresa = new Empresa();
        empresa.id = UUID.randomUUID();
        when(repo.findById(empresa.id)).thenReturn(Optional.of(empresa));
        var dados = new DadosEmpresa("Loja", "", "", "", "", "", "America/Sao_Paulo",
                "#123456", "#abcdef", "", 0);
        var resposta = service.salvarEmpresa(empresa.id, dados, "dono@example.test");
        verify(acesso).exigirVinculo(empresa.id, "dono@example.test");
        verify(repo).save(empresa);
        assertThat(resposta.corPrimaria()).isEqualTo("#123456");
        assertThat(resposta.corSecundaria()).isEqualTo("#abcdef");
        assertThat(resposta.fotoEmpresa()).isEmpty();
    }
    @Test void rejeitaCoresInvalidasNoContrato() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var dados = new DadosEmpresa("Loja", "", "", "", "", "", "America/Sao_Paulo",
                    "red", "url(x)", "", 0);
            assertThat(factory.getValidator().validate(dados)).hasSize(2);
        }
    }
}
