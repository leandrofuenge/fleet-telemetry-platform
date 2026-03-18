package com.app.telemetria.application.scheduler;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.telemetria.domain.service.AlertaService;
import com.app.telemetria.domain.service.VeiculoService;
import com.app.telemetria.domain.service.ViagemService;

@Component
@EnableScheduling
public class AlertaScheduler {

    private final VeiculoService veiculoService;
    private final ViagemService viagemService;

    public AlertaScheduler(
            AlertaService alertaService,
            VeiculoService veiculoService,
            ViagemService viagemService) {
        this.veiculoService = veiculoService;
        this.viagemService = viagemService;
    }

    @Scheduled(fixedDelay = 60000) // A cada 1 minuto
    public void verificarAlertasPeriodicos() {

        veiculoService.listarTodos().forEach(veiculo -> {
            // Lógica para verificar tempo parado
        });

        veiculoService.listarTodos().forEach(veiculo -> {
            // Lógica para verificar última telemetria
        });

        // Verificar atrasos em viagens
        viagemService.listarEmAndamento().forEach(viagem -> {
            // Lógica para verificar atrasos
        });
    }
}
