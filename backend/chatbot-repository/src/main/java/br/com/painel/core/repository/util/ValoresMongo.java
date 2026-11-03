package br.com.painel.core.repository.util;

import org.bson.Document;
import org.springframework.data.mongodb.core.query.*;

import java.time.Instant;
import java.util.*;

public final class ValoresMongo {
    private ValoresMongo() {}

    public static Query consultaPor(String campo, Object valor) {
        return Query.query(Criteria.where(campo).is(valor));
    }

    public static Instant instante(Document documento, String chave) {
        Date valor = documento.getDate(chave);
        return valor == null ? null : valor.toInstant();
    }

    public static long numero(Document documento, String chave) {
        return ((Number) documento.getOrDefault(chave, 0L)).longValue();
    }
}
