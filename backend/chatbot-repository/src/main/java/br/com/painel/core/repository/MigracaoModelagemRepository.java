package br.com.painel.core.repository;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Repository
public class MigracaoModelagemRepository {

    private static final List<String> LEGADAS =
            List.of(
                    "vinculos_usuarios_empresas",
                    "versoes_configuracoes_bot",
                    "convites_empresas",
                    "configuracoes_globais",
                    "historico_prompts_globais",
                    "eventos_auditoria",
                    "integracoes_chatwoot",
                    "eventos_mensagens_atendimento",
                    "provisionamento");

    private final MongoTemplate mongo;

    public MigracaoModelagemRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    private List<Document> documentos(String colecao) {
        return mongo.find(new Query(), Document.class, colecao);
    }

    private Object valor(Document d, String atual, String antigo, Object padrao) {
        return d.containsKey(atual) ? d.get(atual) : d.getOrDefault(antigo, padrao);
    }

    public void converter() {
        var empresas = documentos("empresas");
        var usuarios = documentos("usuarios");
        var vinculos = documentos("vinculos_usuarios_empresas");
        var bots = documentos("versoes_configuracoes_bot");
        var convites = documentos("convites_empresas");
        var idsEmpresas = new HashSet<Object>();
        empresas.forEach(e -> idsEmpresas.add(e.get("_id")));
        // Validate every relationship before modifying any document. Never choose a tenant
        // silently.
        for (var u : usuarios) {
            boolean admin =
                    "SUPERUSUARIO".equals(u.getString("perfil"))
                            || u.getBoolean("superusuario", false);
            var ids = new HashSet<Object>();
            if (u.get("empresaId") != null) {
                ids.add(u.get("empresaId"));
            }
            vinculos.stream()
                    .filter(v -> u.get("_id").equals(v.get("usuario_id")))
                    .forEach(v -> ids.add(v.get("empresa_id")));
            if (!admin && (ids.size() != 1 || !idsEmpresas.containsAll(ids))) {
                throw new IllegalStateException(
                        "Migração interrompida: usuário sem empresa válida ou com múltiplas"
                                + " empresas. ID: "
                                + u.get("_id"));
            }
            if (u.containsKey("perfil")
                    && !Set.of("USUARIO", "SUPERUSUARIO").contains(u.getString("perfil"))) {
                throw new IllegalStateException("Perfil desconhecido: " + u.get("_id"));
            }
        }
        for (var c : convites) {
            if (usuarios.stream().noneMatch(u -> u.get("_id").equals(c.get("usuario_id")))) {
                throw new IllegalStateException("Convite sem usuário: " + c.get("_id"));
            }
        }
        for (var e : empresas) {
            if (bots.stream()
                            .filter(
                                    b ->
                                            e.get("_id").equals(b.get("empresa_id"))
                                                    && "rascunho".equals(b.getString("estado")))
                            .count()
                    > 1) {
                throw new IllegalStateException(
                        "Mais de um rascunho para a empresa " + e.get("_id"));
            }
        }
        for (var u : usuarios) {
            if (u.containsKey("perfil")) {
                continue;
            }
            boolean admin = u.getBoolean("superusuario", false);
            Object empresa =
                    admin
                            ? null
                            : vinculos.stream()
                                    .filter(v -> u.get("_id").equals(v.get("usuario_id")))
                                    .findFirst()
                                    .orElseThrow()
                                    .get("empresa_id");
            var novo =
                    new Document("_id", u.get("_id"))
                            .append("empresaId", empresa)
                            .append("nome", u.getOrDefault("nome", ""))
                            .append("email", u.get("email"))
                            .append("hashSenha", valor(u, "hashSenha", "hash_senha", null))
                            .append("perfil", admin ? "SUPERUSUARIO" : "USUARIO")
                            .append("ativo", u.getBoolean("ativo", false))
                            .append("criadoEm", valor(u, "criadoEm", "criado_em", new Date()))
                            .append(
                                    "atualizadoEm",
                                    valor(u, "atualizadoEm", "atualizado_em", new Date()));
            var convitesUsuario =
                    convites.stream()
                            .filter(c -> u.get("_id").equals(c.get("usuario_id")))
                            .toList();
            if (convitesUsuario.size() > 1) {
                throw new IllegalStateException("Mais de um convite por usuário: " + u.get("_id"));
            }
            if (!convitesUsuario.isEmpty()) {
                var c = convitesUsuario.getFirst();
                if (!Objects.equals(empresa, c.get("empresa_id"))) {
                    throw new IllegalStateException("Convite de outra empresa: " + c.get("_id"));
                }
                novo.append(
                        "convite",
                        new Document("tokenHash", c.get("token_hash"))
                                .append("expiraEm", c.get("expira_em"))
                                .append("usadoEm", c.get("usado_em")));
            }
            mongo.save(novo, "usuarios");
        }
        for (var e : empresas) {
            if (e.containsKey("bot")) {
                continue;
            }
            var b =
                    bots.stream()
                            .filter(v -> e.get("_id").equals(v.get("empresa_id")))
                            .sorted(
                                    Comparator.<Document, Boolean>comparing(
                                                    v -> !"rascunho".equals(v.getString("estado")))
                                            .thenComparing(
                                                    Comparator.comparingInt(
                                                                    (Document v) ->
                                                                            ((Number)
                                                                                            v
                                                                                                    .getOrDefault(
                                                                                                            "versao",
                                                                                                            0))
                                                                                    .intValue())
                                                            .reversed()))
                            .findFirst()
                            .orElse(new Document());
            var bot =
                    new Document("ativo", e.getBoolean("bot_ativo", false))
                            .append("nome", b.getOrDefault("nome_bot", "Assistente"))
                            .append("tomVoz", b.getOrDefault("tom_voz", "cordial"))
                            .append("usarEmojis", b.getBoolean("usar_emojis", false))
                            .append("instrucoes", b.getOrDefault("instrucoes", ""))
                            .append(
                                    "instrucoesTransferencia",
                                    b.getOrDefault("instrucoes_transferencia", ""));
            var novo = new Document("_id", e.get("_id"));
            for (var campo :
                    List.of("nome", "descricao", "endereco", "contato", "site", "servicos")) {
                novo.append(campo, e.getOrDefault(campo, ""));
            }
            var integracao =
                    documentos("integracoes_chatwoot").stream()
                            .filter(v -> e.get("_id").equals(v.get("empresa_id")))
                            .findFirst()
                            .orElse(new Document());
            novo.append("fusoHorario", valor(e, "fusoHorario", "fuso_horario", "America/Sao_Paulo"))
                    .append(
                            "chatwootAccountId",
                            valor(
                                    e,
                                    "chatwootAccountId",
                                    "chatwoot_account_id",
                                    integracao.get("account_id")))
                    .append(
                            "chatwootInboxId",
                            valor(
                                    e,
                                    "chatwootInboxId",
                                    "chatwoot_inbox_id",
                                    integracao.get("inbox_id")))
                    .append("bot", bot)
                    .append("revisao", e.getOrDefault("revisao", 0L))
                    .append("criadoEm", valor(e, "criadoEm", "criado_em", new Date()))
                    .append("atualizadoEm", valor(e, "atualizadoEm", "atualizado_em", new Date()));
            mongo.save(novo, "empresas");
        }
        if (mongo.findById("principal", Document.class, "configuracoes_sistema") == null) {
            var antigos = documentos("configuracoes_globais");
            var g = antigos.isEmpty() ? new Document() : antigos.getFirst();
            mongo.insert(
                    new Document("_id", "principal")
                            .append("promptGlobal", g.getOrDefault("prompt_sistema", ""))
                            .append(
                                    "complementarConfiguracoesEmpresa",
                                    g.getBoolean("complementar_configuracoes_empresa", true))
                            .append("revisao", g.getOrDefault("revisao", 0L))
                            .append("atualizadoEm", g.getOrDefault("atualizado_em", new Date())),
                    "configuracoes_sistema");
        }
        for (var evento : documentos("eventos_mensagens_atendimento")) {
            if (!"enviada".equals(evento.getString("direcao"))
                    || !"sucesso".equals(evento.getString("resultado"))) {
                continue;
            }
            var id = idEvento(evento.get("_id"));
            if (mongo.findById(id, Document.class, "eventos_mensagens") == null) {
                mongo.insert(
                        new Document("_id", id)
                                .append("empresaId", evento.get("empresa_id"))
                                .append("enviadoEm", evento.get("ocorrido_em")),
                        "eventos_mensagens");
            }
        }
    }

    private ObjectId idEvento(Object id) {
        if (id instanceof ObjectId objectId) {
            return objectId;
        }
        try {
            return new ObjectId(
                    Arrays.copyOf(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(id.toString().getBytes(StandardCharsets.UTF_8)),
                            12));
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException(erro);
        }
    }

    public void removerLegadas() {
        for (var colecao : LEGADAS) {
            if (mongo.collectionExists(colecao)) {
                mongo.dropCollection(colecao);
            }
        }
    }
}
