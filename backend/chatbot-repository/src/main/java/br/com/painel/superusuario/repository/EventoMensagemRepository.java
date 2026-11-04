package br.com.painel.superusuario.repository;

import br.com.painel.core.model.exception.*;
import br.com.painel.superusuario.model.EventoMensagem;
import br.com.painel.superusuario.model.vo.DadosEventoMensagem;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public class EventoMensagemRepository {

    private final MongoTemplate mongo;

    public EventoMensagemRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public boolean registrar(String identificador, DadosEventoMensagem evento) {
        var id = new ObjectId(identificador);
        var data = Date.from(evento.enviadoEm());
        try {
            mongo.insert(
                    new Document("_id", id)
                            .append("empresaId", evento.empresaId())
                            .append("enviadoEm", data),
                    "eventos_mensagens");
            return true;
        } catch (DuplicateKeyException erro) {
            var existente = mongo.findById(id, EventoMensagem.class);
            if (existente != null
                    && evento.empresaId().equals(existente.empresaId())
                    && data.equals(Date.from(existente.enviadoEm()))) {
                return false;
            }
            throw new NegocioException(
                    ErroNegocio.CONFLITO, "Este identificador já foi usado para outra mensagem.");
        }
    }
}
