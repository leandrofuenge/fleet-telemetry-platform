package com.telemetria.integration.sefaz.cte;

import java.util.List;


public final class CteSoapClientFactory {
    public CteSoapClient create(CteSoapTransport transport, List<CteSoapInterceptor> interceptors) {
        return new CteSoapClientImpl(transport, interceptors);
    }
}
