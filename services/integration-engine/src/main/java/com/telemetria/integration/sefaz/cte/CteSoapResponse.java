package com.telemetria.integration.sefaz.cte;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record CteSoapResponse(int statusCode, String body, Map<String, List<String>> headers, Duration duration) {
    public CteSoapResponse {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        duration = duration == null ? Duration.ZERO : duration;
    }
    public boolean successful() { return statusCode >= 200 && statusCode < 300; }
}
