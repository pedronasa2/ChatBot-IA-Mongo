package br.com.painel.convites.service;

import br.com.painel.auditoria.repository.AuditoriaRepository;
import br.com.painel.autenticacao.repository.AtivacaoUsuarioRepository;
import br.com.painel.autenticacao.repository.UsuarioRepository;
import br.com.painel.convites.model.vo.AtivacaoConvite;
import br.com.painel.convites.model.vo.ConviteCriado;
import br.com.painel.convites.model.vo.NovaEmpresa;
import br.com.painel.convites.model.vo.SituacaoConvite;
import br.com.painel.convites.repository.CadastroEmpresaRepository;
import br.com.painel.convites.repository.ConviteRepository;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;

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
    private final AtivacaoUsuarioRepository ativacao;
    private final AuditoriaRepository auditoria;
    private final PasswordEncoder encoder;

    public ConviteService(
            ConviteRepository convites,
            UsuarioRepository usuarios,
            CadastroEmpresaRepository cadastro,
            AtivacaoUsuarioRepository ativacao,
            AuditoriaRepository auditoria,
            PasswordEncoder encoder) {
        this.convites = convites;
        this.usuarios = usuarios;
        this.cadastro = cadastro;
        this.ativacao = ativacao;
        this.auditoria = auditoria;
        this.encoder = encoder;
    }

    @Transactional
    public ConviteCriado criar(NovaEmpresa dados, UUID autor) {
        String email = dados.email().strip().toLowerCase(Locale.ROOT);
        try {
            if (usuarios.findByEmail(email).isPresent()) {
                throw new NegocioException(
                        ErroNegocio.CONFLITO,
                        "Este e-mail já possui cadastro. Use outro e-mail para a nova empresa.");
            }
            UUID empresa = UUID.randomUUID(), usuario = UUID.randomUUID();
            cadastro.criar(
                    dados,
                    email,
                    encoder.encode(UUID.randomUUID().toString()),
                    empresa,
                    usuario,
                    autor);
            var convite = gerar(empresa, usuario, email);
            auditoria.auditar(empresa, autor, null, "empresa_criada_convite_simulado");
            return convite;
        } catch (DuplicateKeyException e) {
            throw new NegocioException(ErroNegocio.CONFLITO, "Este e-mail já possui cadastro.");
        }
    }

    private ConviteCriado gerar(UUID empresa, UUID usuario, String email) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expira = Instant.now().plus(Duration.ofHours(48));
        convites.renovar(empresa, usuario, email, hash(token), expira);
        return new ConviteCriado(empresa, email, token, expira, true);
    }

    public SituacaoConvite situacao(UUID empresa) {
        var c = convites.buscar(empresa);
        if (c == null) {
            return new SituacaoConvite("", "", "sem_convite", null);
        }
        var u = usuarios.findById(c.usuarioId).orElse(null);
        String estado =
                c.usadoEm != null
                        ? "aceito"
                        : c.expiraEm.isBefore(Instant.now()) ? "expirado" : "pendente";
        return new SituacaoConvite(c.email, u == null ? "" : u.nome, estado, c.expiraEm);
    }

    @Transactional
    public ConviteCriado reenviar(UUID empresa, UUID autor) {
        var c = convites.buscar(empresa);
        if (c == null) {
            throw new NegocioException(
                    ErroNegocio.NAO_ENCONTRADO, "Esta empresa não possui convite.");
        }
        var u = usuarios.findById(c.usuarioId).orElse(null);
        if (u == null || u.ativo || c.usadoEm != null) {
            throw new NegocioException(ErroNegocio.CONFLITO, "Este convite já foi aceito.");
        }
        var novo = gerar(empresa, u.id, c.email);
        auditoria.auditar(empresa, autor, null, "convite_simulado_renovado");
        return novo;
    }

    @Transactional
    public void ativar(AtivacaoConvite dados) {
        if (!dados.senha().equals(dados.confirmacaoSenha())
                || dados.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS,
                    "As senhas devem coincidir e respeitar o limite de 72 bytes.");
        }
        String senha = encoder.encode(dados.senha());
        var c =
                convites.consumir(
                        hash(dados.token()), dados.email().strip().toLowerCase(Locale.ROOT));
        if (c == null) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS,
                    "Convite inválido, expirado ou já utilizado. Confira o e-mail e peça um novo"
                            + " convite ao administrador.");
        }
        if (!ativacao.ativar(c.usuarioId, senha)) {
            throw new NegocioException(ErroNegocio.CONFLITO, "Esta conta já foi ativada.");
        }
        auditoria.auditar(c.empresaId, c.usuarioId, null, "convite_aceito");
    }

    private static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
