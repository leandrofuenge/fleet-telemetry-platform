package com.telemetria.application.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.infrastructure.persistence.AlertaRepository;

/** Registra os marcos de escalonamento de SLA para alertas ainda não resolvidos. */
@Component
public class AlertaSlaScheduler {
    private final AlertaRepository alertaRepository;
    public AlertaSlaScheduler(AlertaRepository alertaRepository) { this.alertaRepository = alertaRepository; }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void escalarPendentes() {
        LocalDateTime agora = LocalDateTime.now();
        for (Alerta alerta : alertaRepository.findByResolvidoFalseOrderByDataHoraDesc()) {
            long minutos = Duration.between(alerta.getDataHora(), agora).toMinutes();
            Map<String, Object> contexto = alerta.getDadosContexto() == null
                    ? new HashMap<>() : new HashMap<>(alerta.getDadosContexto());
            boolean mudou = false;
            if (deveEscalarOperador(alerta.getSeveridade(), minutos) && !contexto.containsKey("escalonado_operador_em")) {
                contexto.put("escalonado_operador_em", agora.toString()); mudou = true;
            }
            if (deveEscalarAdmin(alerta.getSeveridade(), minutos) && !contexto.containsKey("escalonado_admin_em")) {
                contexto.put("escalonado_admin_em", agora.toString()); mudou = true;
            }
            if (mudou) { alerta.setDadosContexto(contexto); alertaRepository.save(alerta); }
        }
    }

    private boolean deveEscalarOperador(SeveridadeAlerta severidade, long minutos) {
        return (severidade == SeveridadeAlerta.CRITICO && minutos >= 10) ||
                (severidade == SeveridadeAlerta.ALTO && minutos >= 120);
    }
    private boolean deveEscalarAdmin(SeveridadeAlerta severidade, long minutos) {
        return (severidade == SeveridadeAlerta.CRITICO && minutos >= 30) ||
                (severidade == SeveridadeAlerta.ALTO && minutos >= 240);
    }
}
