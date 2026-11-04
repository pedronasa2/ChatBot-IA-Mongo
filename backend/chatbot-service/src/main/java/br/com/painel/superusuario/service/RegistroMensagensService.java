package br.com.painel.superusuario.service;

import br.com.painel.core.model.exception.*;
import br.com.painel.empresas.repository.EmpresaRepository;
import br.com.painel.superusuario.model.vo.DadosEventoMensagem;
import br.com.painel.superusuario.repository.EventoMensagemRepository;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RegistroMensagensService {

    private final EmpresaRepository empresas;

    private final EventoMensagemRepository eventos;

    public RegistroMensagensService(EmpresaRepository empresas, EventoMensagemRepository eventos) {
        this.empresas = empresas;
        this.eventos = eventos;
    }

    public boolean registrar(String identificador, DadosEventoMensagem evento) {
        if (identificador == null || !identificador.matches("[0-9a-fA-F]{24}")) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS,
                    "Idempotency-Key deve conter um ObjectId de 24 caracteres hexadecimais, estável"
                            + " por mensagem.");
        }
        if (evento.enviadoEm().isAfter(Instant.now())) {
            throw new NegocioException(
                    ErroNegocio.DADOS_INVALIDOS, "A mensagem não pode ter uma data futura.");
        }
        if (!empresas.existsById(evento.empresaId())) {
            throw new NegocioException(ErroNegocio.NAO_ENCONTRADO, "Empresa não encontrada.");
        }
        return eventos.registrar(identificador, evento);
    }
}
