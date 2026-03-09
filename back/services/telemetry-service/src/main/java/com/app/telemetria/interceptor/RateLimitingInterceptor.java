package com.app.telemetria.interceptor;

import com.app.telemetria.config.RateLimitingConfig;
import com.app.telemetria.exception.BusinessException;
import com.app.telemetria.exception.ErrorCode;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    private final RateLimitingConfig rateLimitingConfig;
    
    public RateLimitingInterceptor(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Ignorar endpoints públicos
        String path = request.getRequestURI();
        if (path.contains("/auth/login") || path.contains("/auth/refresh")) {
            return true;
        }
        
        // Identificador único (IP ou token)
        String identificador = getIdentificador(request);
        
        Bucket bucket = rateLimitingConfig.resolveBucket(identificador);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Adicionar headers informativos
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            // Calcular tempo de espera
            long nanosParaEsperar = probe.getNanosToWaitForRefill();
            long segundosParaEsperar = Duration.ofNanos(nanosParaEsperar).getSeconds();
            
            response.addHeader("X-Rate-Limit-Retry-After", String.valueOf(segundosParaEsperar));
            response.setStatus(429);
            response.getWriter().write("Muitas requisições. Aguarde " + segundosParaEsperar + " segundos.");
            
            System.err.println("⛔ Rate limit excedido para: " + identificador);
            
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, 
                "Limite de requisições excedido. Aguarde " + segundosParaEsperar + "s");
        }
    }
    
    private String getIdentificador(HttpServletRequest request) {
        // Tenta pegar do header X-Veiculo-Id
        String veiculoId = request.getHeader("X-Veiculo-Id");
        if (veiculoId != null) {
            return "veiculo:" + veiculoId;
        }
        
        // Fallback: IP do cliente
        String ip = request.getRemoteAddr();
        return "ip:" + ip;
    }
}