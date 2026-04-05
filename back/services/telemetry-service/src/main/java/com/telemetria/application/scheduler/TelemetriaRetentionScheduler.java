package com.telemetria.application.scheduler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;

@Component
public class TelemetriaRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaRetentionScheduler.class);

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private TelemetriaRepository telemetriaRepository;

    // Retenção padrão para planos (dias)
    private static final Map<String, Integer> RETENCAO_POR_PLANO = Map.of(
        "STARTER", 60,
        "PRO", 180,
        "ENTERPRISE", 365   // 1 ano padrão (configurável via propriedade)
    );

    // Retenção mínima para dados de jornada (2 anos = 730 dias)
    private static final int RETENCAO_JORNADA_DIAS = 730;

    // Propriedade para sobrescrever retenção do plano ENTERPRISE (máximo 5 anos)
    @Value("${retencao.enterprise.dias:365}")
    private int retencaoEnterpriseDias;

    @Scheduled(cron = "0 0 2 1 * ?") // Executa às 2h do dia 1 de cada mês
    @Transactional
    public void limparTelemetriaAntiga() {
        log.info("🧹 [RF06] Iniciando limpeza mensal de telemetria");

        AtomicInteger totalDeletadosNormais = new AtomicInteger(0);
        AtomicInteger totalDeletadosJornada = new AtomicInteger(0);

        veiculoRepository.findAll().forEach(veiculo -> {
            if (!veiculo.getAtivo()) return;

            // 1. Limpeza de dados normais (não jornada) conforme plano
            int diasRetencao = calcularDiasRetencao(veiculo.getPlano());
            LocalDateTime limiteNormal = LocalDateTime.now().minusDays(diasRetencao);
            int deletadosNormais = telemetriaRepository.deleteDadosNormaisAntigos(veiculo.getId(), limiteNormal);
            totalDeletadosNormais.addAndGet(deletadosNormais);

            if (deletadosNormais > 0) {
                log.info("🗑️ Veículo {} (plano {}): {} registros normais deletados (retenção {} dias)",
                         veiculo.getPlaca(), veiculo.getPlano(), deletadosNormais, diasRetencao);
            }

            // 2. Limpeza de dados de jornada (Lei 12.619) com retenção mínima de 2 anos
            LocalDateTime limiteJornada = LocalDateTime.now().minusDays(RETENCAO_JORNADA_DIAS);
            int deletadosJornada = telemetriaRepository.deleteJornadaAntiga(veiculo.getId(), limiteJornada);
            totalDeletadosJornada.addAndGet(deletadosJornada);

            if (deletadosJornada > 0) {
                log.info("🗑️ Veículo {}: {} registros de jornada deletados (retenção {} dias)",
                         veiculo.getPlaca(), deletadosJornada, RETENCAO_JORNADA_DIAS);
            }
        });

        log.info("🧹 [RF06] Limpeza mensal CONCLUÍDA - Total deletados: {} normais + {} jornada = {} registros",
                 totalDeletadosNormais.get(), totalDeletadosJornada.get(),
                 totalDeletadosNormais.get() + totalDeletadosJornada.get());
    }

    private int calcularDiasRetencao(String plano) {
        if ("ENTERPRISE".equalsIgnoreCase(plano)) {
            // Garante que não ultrapasse 5 anos (1825 dias)
            return Math.min(retencaoEnterpriseDias, 1825);
        }
        return RETENCAO_POR_PLANO.getOrDefault(plano, 60);
    }
}