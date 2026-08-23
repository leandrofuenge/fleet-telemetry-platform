package com.telemetria.integration.sefaz.cte;

import java.util.List;


public final class CteSoapClientImpl implements CteSoapClient {
    private final CteSoapTransport transport;
    private final List<CteSoapInterceptor> interceptors;
    public CteSoapClientImpl(CteSoapTransport transport, List<CteSoapInterceptor> interceptors) {
        this.transport = java.util.Objects.requireNonNull(transport);
        this.interceptors = interceptors == null ? List.of() : List.copyOf(interceptors);
    }
    @Override public CteSoapResponse execute(CteSoapRequest original) {
        CteSoapRequest request = original;
        for (var interceptor : interceptors) request = interceptor.before(request);
        try {
            CteSoapResponse response = transport.send(request);
            for (var interceptor : interceptors) interceptor.after(request, response);
            if (!response.successful()) throw new CteTransportException("A SEFAZ retornou HTTP " + response.statusCode() + ".");
            return response;
        } catch (RuntimeException exception) {
            for (var interceptor : interceptors) interceptor.onError(request, exception);
            throw exception;
        }
    }
}
