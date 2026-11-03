package br.com.painel.superusuario.repository;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import br.com.painel.superusuario.model.vo.ContextoRascunhoPrompt;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ContextoPromptRepository {
    private final MongoTemplate mongo;

    public ContextoPromptRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public ContextoRascunhoPrompt rascunho(UUID empresa) {
        return mongo.findOne(
                consultaPor("empresa_id", empresa)
                        .addCriteria(Criteria.where("estado").is("rascunho")),
                ContextoRascunhoPrompt.class,
                "versoes_configuracoes_bot");
    }
}
