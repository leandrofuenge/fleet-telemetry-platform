package com.telemetria.integration.sefaz.cte;

import java.time.Duration;

public record CteSoapProperties(Duration connectTimeout, Duration readTimeout, String tlsProtocol) {
    public CteSoapProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(15) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(30) : readTimeout;
        tlsProtocol = tlsProtocol == null || tlsProtocol.isBlank() ? "TLSv1.2" : tlsProtocol;
    }
    public static CteSoapProperties defaults() { return new CteSoapProperties(null, null, null); }
}
