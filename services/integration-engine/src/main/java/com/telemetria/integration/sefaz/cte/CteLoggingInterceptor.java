package com.telemetria.integration.sefaz.cte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CteLoggingInterceptor implements CteSoapInterceptor {
    private static final Logger log = LoggerFactory.getLogger(CteLoggingInterceptor.class);
    @Override public CteSoapRequest before(CteSoapRequest request) {
        log.debug("Iniciando SOAP CT-e action={} host={}", request.soapAction(), request.endpoint().getHost());
        return request;
    }
    @Override public void after(CteSoapRequest request, CteSoapResponse response) {
        log.info("SOAP CT-e action={} status={} durationMs={}", request.soapAction(), response.statusCode(), response.duration().toMillis());
    }
    @Override public void onError(CteSoapRequest request, RuntimeException exception) {
        log.warn("Falha SOAP CT-e action={}: {}", request.soapAction(), exception.getMessage());
    }
}
