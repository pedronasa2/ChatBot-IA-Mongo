package br.com.painel.empresas.service;

import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.core.service.security.AcessoEmpresa;
import br.com.painel.core.service.security.AcessoSuperusuario;
import br.com.painel.empresas.model.Empresa;
import br.com.painel.empresas.model.vo.DadosEmpresa;
import br.com.painel.empresas.model.vo.EmpresaResposta;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.superusuario.model.vo.DadosChatwoot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class EmpresaService {

    private final AcessoEmpresa acesso;

    private final EmpresaRepository empresas;

    private final AcessoSuperusuario superusuario;

    public EmpresaService(
            AcessoEmpresa acesso, EmpresaRepository empresas, AcessoSuperusuario superusuario) {
        this.acesso = acesso;
        this.empresas = empresas;
        this.superusuario = superusuario;
    }

    @Transactional(readOnly = true)
    public EmpresaResposta empresa(UUID empresaId, String email) {
        acesso.exigirVinculo(empresaId, email);
        return EmpresaResposta.de(empresas.findById(empresaId).orElseThrow());
    }

    @Transactional
    public EmpresaResposta salvarEmpresa(UUID empresaId, DadosEmpresa dados, String email) {
        UUID usuarioResponsavelId = acesso.exigirVinculo(empresaId, email);
        try {
            ZoneId.of(dados.fusoHorario());
        } catch (DateTimeException erro) {
            throw new NegocioException(ErroNegocio.DADOS_INVALIDOS, "Fuso horário inválido");
        }
        if (!dados.site().isBlank()) {
            try {
                var uri = java.net.URI.create(dados.site());
                if (!Set.of("https", "http").contains(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException erro) {
                throw new NegocioException(
                        ErroNegocio.DADOS_INVALIDOS, "Informe um site com http:// ou https://");
            }
        }
        Empresa empresa = empresas.findById(empresaId).orElseThrow();
        if (empresa.revisao != dados.revisao()) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "Dados alterados em outra sessão. Recarregue antes de salvar.");
        }
        if (dados.fotoEmpresa() != null) {
            validarFoto(dados.fotoEmpresa());
            empresa.fotoEmpresa = dados.fotoEmpresa();
        }
        if (dados.corPrimaria() != null) empresa.corPrimaria = dados.corPrimaria();
        if (dados.corSecundaria() != null) empresa.corSecundaria = dados.corSecundaria();
        empresa.nome = dados.nome().strip();
        empresa.descricao = dados.descricao();
        empresa.endereco = dados.endereco();
        empresa.contato = dados.contato();
        empresa.site = dados.site();
        empresa.servicos = dados.servicos();
        empresa.fusoHorario = dados.fusoHorario();
        empresa.atualizadoEm = Instant.now();
        empresas.save(empresa);
        return EmpresaResposta.de(empresa);
    }

    static void validarFoto(String foto) {
        if (foto.isEmpty()) return;
        try {
            if (!foto.matches("data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+")) throw new IllegalArgumentException();
            byte[] bytes = Base64.getDecoder().decode(foto.substring(foto.indexOf(',') + 1));
            if (bytes.length > 262144) throw new IllegalArgumentException();
            try (var input = javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                var leitores = javax.imageio.ImageIO.getImageReaders(input);
                if (!leitores.hasNext()) throw new IllegalArgumentException();
                var leitor = leitores.next();
                try {
                    leitor.setInput(input);
                    String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                    if (!Set.of("png", "jpeg").contains(formato)
                            || !foto.startsWith("data:image/" + formato + ";")
                            || leitor.getWidth(0) > 512 || leitor.getHeight(0) > 512
                            || leitor.read(0) == null) throw new IllegalArgumentException();
                } finally { leitor.dispose(); }
            }
        } catch (Exception erro) {
            throw new NegocioException(ErroNegocio.DADOS_INVALIDOS,
                    "Use uma foto PNG ou JPEG de até 512 × 512 pixels e 256 KB.");
        }
    }

    public EmpresaResposta salvarChatwoot(UUID id, DadosChatwoot dados, String email) {
        superusuario.exigir(() -> email);
        var e =
            empresas.findById(id).orElseThrow(
                () -> new NegocioException(
                            ErroNegocio.NAO_ENCONTRADO,
                            "Empresa não encontrada."));

        if (e.revisao != dados.revisao()) {
            throw new NegocioException(
                ErroNegocio.CONFLITO,
                "Dados alterados em outra sessão. Recarregue antes de salvar.");
        }
        e.chatwootAccountId = dados.chatwootAccountId();
        e.chatwootInboxId = dados.chatwootInboxId();
        e.atualizadoEm = Instant.now();
        try {
            return EmpresaResposta.de(empresas.save(e));
        } catch (org.springframework.dao.DuplicateKeyException erro) {
            throw new NegocioException(
                ErroNegocio.CONFLITO,
                "Esta conta e caixa do Chatwoot já estão vinculadas a outra empresa.");
        }
    }
}
