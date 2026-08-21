package com.telemetria.integration.support.observability;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpFlowLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final Logger log = LoggerFactory.getLogger(HttpFlowLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/prometheus");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String correlationId = normalizeCorrelationId(request.getHeader(CORRELATION_HEADER));
        long started = System.nanoTime();
        response.setHeader(CORRELATION_HEADER, correlationId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", correlationId)) {
            log.info("HTTP IN  method={} path={} remote={}", request.getMethod(),
                    request.getRequestURI(), request.getRemoteAddr());
            try {
                chain.doFilter(request, response);
            } finally {
                long durationMs = (System.nanoTime() - started) / 1_000_000;
                if (response.getStatus() >= 500) {
                    log.error("HTTP OUT method={} path={} status={} durationMs={}", request.getMethod(),
                            request.getRequestURI(), response.getStatus(), durationMs);
                } else if (response.getStatus() >= 400) {
                    log.warn("HTTP OUT method={} path={} status={} durationMs={}", request.getMethod(),
                            request.getRequestURI(), response.getStatus(), durationMs);
                } else {
                    log.info("HTTP OUT method={} path={} status={} durationMs={}", request.getMethod(),
                            request.getRequestURI(), response.getStatus(), durationMs);
                }
            }
        }
    }

    private String normalizeCorrelationId(String value) {
        if (value == null || value.isBlank() || value.length() > 128
                || !value.matches("[A-Za-z0-9._:-]+")) {
            return UUID.randomUUID().toString();
        }
        return value;
    }
}
