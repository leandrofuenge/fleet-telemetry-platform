package com.telemetria.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class TelemetriaMetrics {

    private final Counter mensagensProcessadas;
    private final Counter mensagensDescartadas;
    private final Timer tempoProcessamento;

    public TelemetriaMetrics(MeterRegistry registry) {
        this.mensagensProcessadas = Counter.builder("telemetria.processadas")
                .description("Total de mensagens de telemetria processadas com sucesso")
                .register(registry);
        this.mensagensDescartadas = Counter.builder("telemetria.descartadas")
                .description("Total de mensagens descartadas")
                .register(registry);
        this.tempoProcessamento = Timer.builder("telemetria.processing.time")
                .description("Tempo de processamento por mensagem")
                .register(registry);
    }

    public void incrementarProcessadas() {
        mensagensProcessadas.increment();
    }

    public void incrementarDescartadas() {
        mensagensDescartadas.increment();
    }

    public Timer.Sample iniciarTimer() {
        return Timer.start();
    }

    public void pararTimer(Timer.Sample sample) {
        sample.stop(tempoProcessamento);
    }
}