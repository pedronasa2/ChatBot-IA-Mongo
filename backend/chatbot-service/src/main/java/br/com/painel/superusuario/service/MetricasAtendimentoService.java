package br.com.painel.superusuario.service;

import br.com.painel.superusuario.model.vo.ResumoMetricas;
import br.com.painel.superusuario.repository.MetricasAtendimentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class MetricasAtendimentoService {
    private final MetricasAtendimentoRepository metricas;

    public MetricasAtendimentoService(MetricasAtendimentoRepository metricas) {
        this.metricas = metricas;
    }

    @Transactional(readOnly = true)
    public ResumoMetricas consultar() {
        return metricas.consultar(Instant.now());
    }
}
