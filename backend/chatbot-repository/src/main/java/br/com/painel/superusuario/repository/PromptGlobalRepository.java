package br.com.painel.superusuario.repository;

import static br.com.painel.core.repository.util.ValoresMongo.*;

import br.com.painel.autenticacao.repository.UsuarioRepository;
import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.superusuario.model.vo.DadosPromptGlobal;
import br.com.painel.superusuario.model.vo.PromptGlobal;
import br.com.painel.superusuario.model.vo.VersaoPromptGlobal;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class PromptGlobalRepository {
    private final MongoTemplate mongo;
    private final UsuarioRepository usuarios;

    public PromptGlobalRepository(MongoTemplate mongo, UsuarioRepository usuarios) {
        this.mongo = mongo;
        this.usuarios = usuarios;
    }

    private String email(Object id) {
        return id == null ? null : usuarios.findById((UUID) id).map(u -> u.email).orElse(null);
    }

    private List<Document> historicos() {
        return mongo.find(
                new Query().with(Sort.by(Sort.Direction.DESC, "revisao")).limit(20),
                Document.class,
                "historico_prompts_globais");
    }

    public PromptGlobal consultar() {
        var g = mongo.findOne(consultaPor("_id", 1), Document.class, "configuracoes_globais");
        return new PromptGlobal(
                g.getString("prompt_sistema"),
                g.getBoolean("complementar_configuracoes_empresa", true),
                numero(g, "revisao"),
                instante(g, "atualizado_em"),
                email(g.get("atualizado_por")));
    }

    public PromptGlobal salvar(DadosPromptGlobal dados, UUID usuarioId) {
        String prompt = dados.promptSistema();
        long alterados =
                mongo.updateFirst(
                                consultaPor("_id", 1)
                                        .addCriteria(Criteria.where("revisao").is(dados.revisao())),
                                new Update()
                                        .set("prompt_sistema", prompt)
                                        .set(
                                                "complementar_configuracoes_empresa",
                                                dados.complementarConfiguracoesEmpresa())
                                        .inc("revisao", 1)
                                        .set("atualizado_por", usuarioId)
                                        .set("atualizado_em", new java.util.Date()),
                                "configuracoes_globais")
                        .getModifiedCount();
        if (alterados != 1) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "O prompt global foi alterado em outra sessão. Recarregue antes de salvar.");
        }
        mongo.insert(
                new Document("_id", UUID.randomUUID())
                        .append("prompt_sistema", prompt)
                        .append(
                                "complementar_configuracoes_empresa",
                                dados.complementarConfiguracoesEmpresa())
                        .append("revisao", dados.revisao() + 1)
                        .append("usuario_responsavel_id", usuarioId)
                        .append("criado_em", new java.util.Date()),
                "historico_prompts_globais");
        return consultar();
    }

    public List<VersaoPromptGlobal> historico() {
        var historicos = historicos();
        var autores =
                usuarios
                        .findAllById(
                                historicos.stream()
                                        .map(h -> h.get("usuario_responsavel_id", UUID.class))
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(u -> u.id, u -> u.email));
        return historicos.stream()
                .map(
                        h ->
                                new VersaoPromptGlobal(
                                        h.getString("prompt_sistema"),
                                        h.getBoolean("complementar_configuracoes_empresa", true),
                                        numero(h, "revisao"),
                                        instante(h, "criado_em"),
                                        autores.get(h.get("usuario_responsavel_id"))))
                .toList();
    }
}
