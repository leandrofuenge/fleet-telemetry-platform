package com.telemetria.domain.service;

import java.util.concurrent.atomic.AtomicLong;

public class MetricasGsm {

    private final AtomicLong mensagensBufferizadas =
            new AtomicLong();

    private final AtomicLong mensagensReenviadas =
            new AtomicLong();

    private final AtomicLong mensagensDescartadas =
            new AtomicLong();

    public void incrementarBufferizadas() {
        mensagensBufferizadas.incrementAndGet();
    }

    public void incrementarReenviadas(long quantidade) {
        mensagensReenviadas.addAndGet(quantidade);
    }

    public void incrementarDescartadas() {
        mensagensDescartadas.incrementAndGet();
    }

    public long getMensagensBufferizadas() {
        return mensagensBufferizadas.get();
    }

    public long getMensagensReenviadas() {
        return mensagensReenviadas.get();
    }

    public long getMensagensDescartadas() {
        return mensagensDescartadas.get();
    }

    @Override
    public String toString() {

        return String.format(
                "Bufferizadas=%d | Reenviadas=%d | Descartadas=%d",
                getMensagensBufferizadas(),
                getMensagensReenviadas(),
                getMensagensDescartadas()
        );
    }
}