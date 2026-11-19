package br.com.painel.superusuario.service;

import br.com.painel.superusuario.repository.UsoSistemaRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

@Service
public class UsoSistemaService {

    private static final Log LOG = LogFactory.getLog(UsoSistemaService.class);

    private final UsoSistemaRepository uso;
    private final boolean habilitado;

    public UsoSistemaService(
            UsoSistemaRepository uso,
            @Value("${painel.uso.bloom-habilitado:false}") boolean habilitado) {
        this.uso = uso;
        this.habilitado = habilitado;
    }

    public void registrarLogin(String email) {
        if (habilitado) {
            try {
                uso.registrarLogin(email.toLowerCase(Locale.ROOT));
            } catch (DataAccessException e) {
                LOG.warn("Redis indisponível para registrar a métrica de login.");
            }
        }
    }

    public Optional<Boolean> possivelmenteFezLogin(String email) {
        if (!habilitado) {
            return Optional.empty();
        }
        try {
            return Optional.of(uso.possivelmenteFezLogin(email.strip().toLowerCase(Locale.ROOT)));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    public OptionalLong estimarUsuarios() {
        if (!habilitado) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(uso.estimarUsuarios());
        } catch (DataAccessException e) {
            return OptionalLong.empty();
        }
    }
}
