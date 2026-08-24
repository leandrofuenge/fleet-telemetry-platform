package com.telemetria.integration.senatran.serpro.api;
import java.time.Instant;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.telemetria.integration.senatran.serpro.domain.InvalidVehicleQueryException;
import com.telemetria.integration.senatran.serpro.domain.SerproIntegrationException;

@RestControllerAdvice(assignableTypes = SerproConsultaController.class)
public class SerproExceptionHandler {
    @ExceptionHandler(InvalidVehicleQueryException.class)
    ResponseEntity<ApiError> invalid(InvalidVehicleQueryException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_VEHICLE_QUERY", exception.getMessage());
    }

    @ExceptionHandler(SerproIntegrationException.class)
    ResponseEntity<ApiError> unavailable(SerproIntegrationException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "SERPRO_UNAVAILABLE", exception.getMessage());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), code, message, MDC.get("correlationId")));
    }

    record ApiError(Instant timestamp, int status, String code, String message, String correlationId) {}
}
