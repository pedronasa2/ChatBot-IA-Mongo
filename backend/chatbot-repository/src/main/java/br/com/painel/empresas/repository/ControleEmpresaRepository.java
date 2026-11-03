package br.com.painel.empresas.repository;

import static br.com.painel.core.repository.util.ValoresMongo.consultaPor;

import br.com.painel.core.model.security.Modulo;
import br.com.painel.empresas.model.vo.PermissoesModulos;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ControleEmpresaRepository {
    private final MongoTemplate mongo;

    public ControleEmpresaRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public boolean atualizarPermissoes(UUID id, PermissoesModulos dados) {
        return mongo.updateFirst(
                                consultaPor("_id", id)
                                        .addCriteria(Criteria.where("revisao").is(dados.revisao())),
                                new Update()
                                        .set(
                                                "modulo_candidatos_habilitado",
                                                dados.moduloCandidatosHabilitado())
                                        .set(
                                                "modulo_agenda_habilitado",
                                                dados.moduloAgendaHabilitado())
                                        .inc("revisao", 1)
                                        .set("atualizado_em", new Date()),
                                "empresas")
                        .getModifiedCount()
                == 1;
    }

    /** Participates in the caller's transaction; conflicts with module deactivation. */
    public void registrarOperacaoModulo(UUID id, Modulo modulo) {
        String campo =
                modulo == Modulo.CANDIDATOS
                        ? "modulo_candidatos_habilitado"
                        : "modulo_agenda_habilitado";
        mongo.updateFirst(
                consultaPor("_id", id).addCriteria(Criteria.where(campo).is(true)),
                new Update().inc("controle_acesso_modulos", 1),
                "empresas");
    }
}
