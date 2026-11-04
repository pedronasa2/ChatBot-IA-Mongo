package br.com.painel.convites.service;

import br.com.painel.autenticacao.repository.UsuarioRepository;
import br.com.painel.convites.model.vo.*;
import br.com.painel.convites.repository.*;
import br.com.painel.core.model.exception.*;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class ConviteService {

    private final ConviteRepository convites;

    private final UsuarioRepository usuarios;

    private final CadastroEmpresaRepository cadastro;

    private final PasswordEncoder encoder;

    public ConviteService(
            ConviteRepository convites,
            UsuarioRepository usuarios,
            CadastroEmpresaRepository cadastro,
            PasswordEncoder encoder) {
        this.convites = convites;
        this.usuarios = usuarios;
        this.cadastro = cadastro;
        this.encoder = encoder;
    }

    @Transactional
    public ConviteCriado criar(NovaEmpresa dados, UUID autor) {
        var email = dados.email().strip().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmail(email).isPresent()) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "Este e-mail já possui cadastro. Use outro e-mail para a nova empresa.");
        }
        try {
            UUID empresa = UUID.randomUUID(), usuario = UUID.randomUUID();
            cadastro.criar(
                    dados, email, encoder.encode(UUID.randomUUID().toString()), empresa, usuario);
            return gerar(empresa, usuario, email);
        } catch (DuplicateKeyException erro) {
            throw new NegocioException(ErroNegocio.CONFLITO, "Este e-mail já possui cadastro.");
        }
    }

    private ConviteCriado gerar(UUID empresa, UUID usuario, String email) {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expira = Instant.now().plus(Duration.ofHours(48));
        if (!convites.renovar(usuario, hash(token), expira)) {
            throw new NegocioException(ErroNegocio.CONFLITO, "Este convite já foi aceito.");
        }
        return new ConviteCriado(empresa, email, token, expira, true);
    }

    public SituacaoConvite situacao(UUID empresa) {
        var u = convites.buscar(empresa);
        if (u == null) {
            return new SituacaoConvite("", "", "sem_convite", null);
        }
        var c = u.convite;
        var estado =
                c.usadoEm != null
                        ? "aceito"
                        : !c.expiraEm.isAfter(Instant.now()) ? "expirado" : "pendente";
        return new SituacaoConvite(u.email, u.nome, estado, c.expiraEm);
    }

    public ConviteCriado reenviar(UUID empresa, UUID autor) {
        var u = convites.buscar(empresa);
        if (u == null) {
            throw new NegocioException(
                    ErroNegocio.NAO_ENCONTRADO, "Esta empresa não possui convite.");
        }
        if (u.ativo || u.convite.usadoEm != null) {
            throw new NegocioException(ErroNegocio.CONFLITO, "Este convite já foi aceito.");
        }
        return gerar(empresa, u.id, u.email);
    }

    public void ativar(AtivacaoConvite dados) {
        if (!dados.senha().equals(dados.confirmacaoSenha())
                || dados.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS,
                    "As senhas devem coincidir e respeitar o limite de 72 bytes.");
        }
        if (!convites.ativar(
                hash(dados.token()),
                dados.email().strip().toLowerCase(Locale.ROOT),
                encoder.encode(dados.senha()))) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS,
                    "Convite inválido, expirado ou já utilizado. Confira o e-mail e peça um novo"
                            + " convite ao administrador.");
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException(erro);
        }
    }
}
