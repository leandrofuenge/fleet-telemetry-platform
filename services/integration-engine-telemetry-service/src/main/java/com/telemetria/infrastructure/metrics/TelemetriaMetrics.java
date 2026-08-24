package com.telemetria.infrastructure.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class TelemetriaMetrics {

    private final Counter mensagensProcessadas;
    private final Counter mensagensDescartadas;
    private final Counter mensagensRecebidas;
    private final Counter mensagensDuplicadas;
    private final Counter mensagensForaDeOrdem;
    private final Counter sequenciasAusentes;
    private final Counter retries;
    private final Counter enviadasDlq;
    private final Timer tempoProcessamento;

    public TelemetriaMetrics(MeterRegistry registry) {
        this.mensagensProcessadas = Counter.builder("telemetria.processadas")
                .description("Total de mensagens de telemetria processadas com sucesso")
                .register(registry);
        this.mensagensDescartadas = Counter.builder("telemetria.descartadas")
                .description("Total de mensagens descartadas")
                .register(registry);
        this.mensagensRecebidas = Counter.builder("telemetria.recebidas")
                .description("Total de mensagens recebidas do Kafka")
                .register(registry);
        this.mensagensDuplicadas = Counter.builder("telemetria.duplicadas")
                .description("Total de mensagens idempotentes ignoradas")
                .register(registry);
        this.mensagensForaDeOrdem = Counter.builder("telemetria.fora.ordem")
                .description("Total de mensagens persistidas fora de ordem")
                .register(registry);
        this.sequenciasAusentes = Counter.builder("telemetria.sequencias.ausentes")
                .description("Quantidade de números de sequência ausentes detectados")
                .register(registry);
        this.retries = Counter.builder("telemetria.kafka.retries")
                .description("Total de tentativas adicionais do consumidor Kafka")
                .register(registry);
        this.enviadasDlq = Counter.builder("telemetria.kafka.dlq")
                .description("Total de mensagens recuperadas na DLQ")
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

    public void incrementarRecebidas() {
        mensagensRecebidas.increment();
    }

    public void incrementarDuplicadas() {
        mensagensDuplicadas.increment();
    }

    public void incrementarForaDeOrdem() {
        mensagensForaDeOrdem.increment();
    }

    public void registrarSequenciasAusentes(long gap) {
        if (gap > 0) sequenciasAusentes.increment(gap);
    }

    public void incrementarRetries() {
        retries.increment();
    }

    public void incrementarDlq() {
        enviadasDlq.increment();
    }

    public Timer.Sample iniciarTimer() {
        return Timer.start();
    }

    public void pararTimer(Timer.Sample sample) {
        sample.stop(tempoProcessamento);
    }
}
