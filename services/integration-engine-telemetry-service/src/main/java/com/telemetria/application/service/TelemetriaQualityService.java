package com.telemetria.application.service;

import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;

@Service
public class TelemetriaQualityService {

    public int evaluate(Telemetria telemetry) {
        int score = 100;

        if (Boolean.TRUE.equals(telemetry.getForaDeOrdem())) score -= 20;
        if (telemetry.getSequenceGap() != null && telemetry.getSequenceGap() > 0) score -= 10;
        if (Boolean.TRUE.equals(telemetry.getImpreciso())) score -= 20;
        if (Boolean.TRUE.equals(telemetry.getAdulteracaoGps())) score -= 35;

        if (telemetry.getHdop() != null) {
            if (telemetry.getHdop() > 10) score -= 25;
            else if (telemetry.getHdop() > 5) score -= 10;
        }
        if (telemetry.getSatelites() != null) {
            if (telemetry.getSatelites() < 4) score -= 20;
            else if (telemetry.getSatelites() < 6) score -= 5;
        }
        if (telemetry.getPrecisaoGps() != null && telemetry.getPrecisaoGps() > 50) score -= 15;
        if (telemetry.getDelaySincronizacaoS() != null && telemetry.getDelaySincronizacaoS() > 300) score -= 10;
        if (telemetry.getDeviceId() == null || telemetry.getDeviceId().isBlank()) score -= 5;

        score = Math.max(0, Math.min(100, score));
        telemetry.setQualidadeDados(score);
        return score;
    }
}
