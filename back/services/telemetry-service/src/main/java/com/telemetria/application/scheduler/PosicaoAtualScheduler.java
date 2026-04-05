package com.telemetria.application.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.service.AlertaService;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;

import java.util.List;

@Component
public class PosicaoAtualScheduler {

    private static final Logger log = LoggerFactory.getLogger(PosicaoAtualScheduler.class);

    @Autowired
    private TelemetriaRepository telemetriaRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private AlertaService alertaService;

    @Value("${posicao.status.desconhecido.minutos:5}")
    private int minutosParaDesconhecido;

    @Value("${posicao.alerta.sem.sinal.minutos:30}")
    private int minutosParaAlertaSemSinal;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void atualizarStatusDesconhecido() {
        log.debug("🔄 Marcando veículos sem telemetria há > {} min como DESCONHECIDO", minutosParaDesconhecido);
        int updated = telemetriaRepository.atualizarStatusDesconhecido(minutosParaDesconhecido);
        if (updated > 0) {
            log.info("✅ {} veículos marcados como DESCONHECIDO", updated);
        }
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void gerarAlertasVeiculoSemSinal() {
        log.debug("🔍 Verificando veículos sem sinal há > {} min com ignição ligada", minutosParaAlertaSemSinal);
        List<Long> veiculosSemSinal = telemetriaRepository.findVeiculosSemSinal(minutosParaAlertaSemSinal, true);
        
        for (Long veiculoId : veiculosSemSinal) {
            veiculoRepository.findById(veiculoId).ifPresent(veiculo -> {
                alertaService.criarAlertaVeiculoSemSinal(veiculoId, veiculo.getTenantId(), veiculo.getUuid(), veiculo.getPlaca());
                log.info("🚨 Alerta VEICULO_SEM_SINAL gerado para veículo {} ({})", veiculoId, veiculo.getPlaca());
            });
        }
    }
}