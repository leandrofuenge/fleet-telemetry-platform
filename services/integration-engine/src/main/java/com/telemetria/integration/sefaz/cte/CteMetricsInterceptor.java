package com.telemetria.integration.sefaz.cte;

import java.util.concurrent.atomic.AtomicLong;

public final class CteMetricsInterceptor implements CteSoapInterceptor {
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    @Override public void after(CteSoapRequest request, CteSoapResponse response) {
        if (response.successful()) successes.incrementAndGet(); else failures.incrementAndGet();
    }
    @Override public void onError(CteSoapRequest request, RuntimeException exception) { failures.incrementAndGet(); }
    public long successes() { return successes.get(); }
    public long failures() { return failures.get(); }
}
