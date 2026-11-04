package br.com.painel.superusuario.repository;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import br.com.painel.core.model.exception.*;
import br.com.painel.superusuario.model.ConfiguracaoSistema;
import br.com.painel.superusuario.model.vo.DadosPromptGlobal;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class PromptGlobalRepository {

    private final MongoTemplate mongo;

    public PromptGlobalRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public ConfiguracaoSistema consultar() {
        return mongo.findById("principal", ConfiguracaoSistema.class);
    }

    public ConfiguracaoSistema salvar(DadosPromptGlobal dados) {
        long alterados =
                mongo.updateFirst(
                                consultaPor("_id", "principal")
                                        .addCriteria(Criteria.where("revisao").is(dados.revisao())),
                                new Update()
                                        .set("promptGlobal", dados.promptGlobal())
                                        .set(
                                                "complementarConfiguracoesEmpresa",
                                                dados.complementarConfiguracoesEmpresa())
                                        .inc("revisao", 1)
                                        .set("atualizadoEm", Instant.now()),
                                ConfiguracaoSistema.class)
                        .getModifiedCount();
        if (alterados != 1) {
            throw new NegocioException(
                    ErroNegocio.CONFLITO,
                    "O prompt global foi alterado em outra sessão. Recarregue antes de salvar.");
        }
        return consultar();
    }
}
