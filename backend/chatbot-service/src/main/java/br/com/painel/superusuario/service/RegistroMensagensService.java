package br.com.painel.superusuario.service;

import br.com.painel.core.model.exception.ErroNegocio;
import br.com.painel.core.model.exception.NegocioException;
import br.com.painel.superusuario.model.vo.EventoMensagem;
import br.com.painel.superusuario.repository.EventoMensagemRepository;

import jakarta.validation.constraints.*;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RegistroMensagensService {
    private final br.com.painel.empresas.repository.EmpresaRepository empresas;
    private final EventoMensagemRepository eventos;

    public RegistroMensagensService(
            br.com.painel.empresas.repository.EmpresaRepository empresas,
            EventoMensagemRepository eventos) {
        this.empresas = empresas;
        this.eventos = eventos;
    }

    public boolean registrar(EventoMensagem evento) {
        if (evento.ocorridoEm().isAfter(Instant.now().plusSeconds(5))) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS, "O evento não pode ter uma data futura.");
        }
        if (!empresas.existsById(evento.empresaId())) {
            throw new NegocioException(ErroNegocio.NAO_ENCONTRADO, "Empresa não encontrada.");
        }
        return eventos.registrar(evento);
    }
}
