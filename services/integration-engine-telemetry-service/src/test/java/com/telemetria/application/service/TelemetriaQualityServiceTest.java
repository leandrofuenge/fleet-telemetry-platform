package com.telemetria.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.telemetria.domain.entity.Telemetria;

class TelemetriaQualityServiceTest {

    private final TelemetriaQualityService service = new TelemetriaQualityService();

    @Test
    void shouldPenalizeLateAndLowPrecisionTelemetry() {
        Telemetria telemetry = new Telemetria();
        telemetry.setDeviceId("dev-1");
        telemetry.setForaDeOrdem(true);
        telemetry.setImpreciso(true);
        telemetry.setHdop(12.0);
        telemetry.setSatelites(3);

        int quality = service.evaluate(telemetry);

        assertThat(quality).isEqualTo(15);
        assertThat(telemetry.getQualidadeDados()).isEqualTo(15);
    }
}
