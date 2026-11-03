package br.com.painel.superusuario.repository;

import br.com.painel.superusuario.model.vo.EventoMensagem;

import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class EventoMensagemRepository {
    private final MongoTemplate mongo;

    public EventoMensagemRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public boolean registrar(EventoMensagem evento) {
        try {
            mongo.insert(
                    new Document("_id", UUID.randomUUID())
                            .append("empresa_id", evento.empresaId())
                            .append("origem", evento.origem().strip())
                            .append("id_evento_origem", evento.idEventoOrigem().strip())
                            .append("direcao", evento.direcao())
                            .append("resultado", evento.resultado())
                            .append("ocorrido_em", Date.from(evento.ocorridoEm()))
                            .append("registrado_em", new Date()),
                    "eventos_mensagens_atendimento");
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
