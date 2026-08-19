package com.telemetria.integration.senatran.serpro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(20)
public class SerproApiKeyFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Integration-API-Key";
    private final SerproProperties properties;

    public SerproApiKeyFilter(SerproProperties properties) { this.properties = properties; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/integracoes/senatran/serpro/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String configured = properties.getSerpro().getApiKey();
        if (configured == null || configured.isBlank()) {
            reject(response, 503, "SERPRO_ENDPOINT_NOT_CONFIGURED");
            return;
        }
        String supplied = request.getHeader(HEADER);
        boolean valid = supplied != null && MessageDigest.isEqual(
                configured.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            reject(response, 401, "UNAUTHORIZED");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\"}");
    }
}
