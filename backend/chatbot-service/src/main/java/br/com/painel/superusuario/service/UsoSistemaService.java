package br.com.painel.superusuario.service;

import br.com.painel.superusuario.repository.UsoSistemaRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.OptionalLong;

@Service
public class UsoSistemaService {

    private static final Log LOG = LogFactory.getLog(UsoSistemaService.class);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final UsoSistemaRepository uso;
    private final boolean habilitado;

    public UsoSistemaService(
            UsoSistemaRepository uso,
            @Value("${painel.uso.bloom-habilitado:false}") boolean habilitado) {
        this.uso = uso;
        this.habilitado = habilitado;
    }

    public LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    public void registrarLogin(String email) {
        if (habilitado) {
            try {
                uso.registrarLogin(hoje(), email);
            } catch (DataAccessException e) {
                LOG.warn("Redis indisponível para registrar a métrica de login.");
            }
        }
    }

    public OptionalLong estimarUsuarios(LocalDate dia) {
        if (!habilitado) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(uso.estimarUsuarios(dia));
        } catch (DataAccessException e) {
            return OptionalLong.empty();
        }
    }
}
