package com.telemetria.integration.sefaz.cte;

import javax.net.ssl.SSLContext;

public final class CteSslContextFactory {
    public SSLContext createDefault(CteSoapProperties properties) {
        try {
            return SSLContext.getInstance(properties.tlsProtocol());
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível criar o SSLContext CT-e.", exception);
        }
    }
}
