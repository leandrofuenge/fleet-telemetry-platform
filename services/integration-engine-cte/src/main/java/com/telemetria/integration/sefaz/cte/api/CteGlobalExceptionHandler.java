package com.telemetria.integration.sefaz.cte.api;

import java.net.URI;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.exception.CteOperationBlockedException;

@RestControllerAdvice
public class CteGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CteGlobalExceptionHandler.class);

    @ExceptionHandler(CteOperationBlockedException.class)
    public ProblemDetail handleOperationBlocked(CteOperationBlockedException ex) {
        log.warn("Operação fiscal bloqueada por regra de segurança: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Operação Fiscal Bloqueada");
        problem.setType(URI.create("https://telemetria.com/errors/cte-operacao-bloqueada"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CteException.class)
    public ProblemDetail handleCteException(CteException ex) {
        log.error("Erro na integração CT-e: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Erro de Integração CT-e");
        problem.setType(URI.create("https://telemetria.com/errors/cte-erro-integracao"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
