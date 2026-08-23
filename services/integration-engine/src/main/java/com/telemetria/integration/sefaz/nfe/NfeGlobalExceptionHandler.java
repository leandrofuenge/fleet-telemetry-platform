package com.telemetria.integration.sefaz.nfe;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte erros NF-e em respostas seguras e previsíveis para a API interna. */
@RestControllerAdvice(assignableTypes = NfeController.class)
public class NfeGlobalExceptionHandler {

    @ExceptionHandler(NfeOperationBlockedException.class)
    ProblemDetail operacaoBloqueada(NfeOperationBlockedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Operação Fiscal NF-e Bloqueada", exception.getMessage(), "operacao-bloqueada");
    }

    @ExceptionHandler(NfeSefazUnavailableException.class)
    ProblemDetail sefazIndisponivel(NfeSefazUnavailableException exception) {
        return problem(HttpStatus.BAD_GATEWAY, "SEFAZ NF-e Indisponível", exception.getMessage(), "sefaz-indisponivel");
    }

    @ExceptionHandler({NfeException.class, IllegalArgumentException.class})
    ProblemDetail entradaInvalida(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição NF-e Inválida", exception.getMessage(), "requisicao-invalida");
    }

    private ProblemDetail problem(HttpStatus status, String titulo, String detalhe, String tipo) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
        problem.setTitle(titulo);
        problem.setType(URI.create("https://telemetria.com/errors/nfe-" + tipo));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
