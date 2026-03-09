package com.app.telemetria.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ================= BUSINESS EXCEPTIONS =================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                ex.getMessage(),
                errorCode.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    // ================= VALIDAÇÃO =================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));

        ErrorResponse response = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    // ================= AUTENTICAÇÃO (401) =================
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.INVALID_CREDENTIALS.getCode(),
                ErrorCode.INVALID_CREDENTIALS.getMessage(),
                ErrorCode.INVALID_CREDENTIALS.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.INVALID_CREDENTIALS.getHttpStatus())
                .body(response);
    }

    // ================= CONTA DESABILITADA (403) =================
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.ACCOUNT_DISABLED.getCode(),
                ErrorCode.ACCOUNT_DISABLED.getMessage(),
                ErrorCode.ACCOUNT_DISABLED.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.ACCOUNT_DISABLED.getHttpStatus())
                .body(response);
    }

    // ================= CONTA BLOQUEADA (403) =================
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.ACCOUNT_LOCKED.getCode(),
                ErrorCode.ACCOUNT_LOCKED.getMessage(),
                ErrorCode.ACCOUNT_LOCKED.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.ACCOUNT_LOCKED.getHttpStatus())
                .body(response);
    }

    // ================= JWT TOKEN INVALID (403) =================
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.TOKEN_INVALID.getCode(),
                ErrorCode.TOKEN_INVALID.getMessage(),
                ErrorCode.TOKEN_INVALID.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.TOKEN_INVALID.getHttpStatus())
                .body(response);
    }

    // ================= RECURSO NÃO ENCONTRADO (404) =================
    @ExceptionHandler({
        com.app.telemetria.exception.MotoristaNotFoundException.class,
        com.app.telemetria.exception.VeiculoNotFoundException.class,
        com.app.telemetria.exception.RotaNotFoundException.class,
        com.app.telemetria.exception.ViagemNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            RuntimeException ex,
            HttpServletRequest request) {

        ErrorCode errorCode = determinarErrorCode(ex);

        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage(),
                errorCode.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    private ErrorCode determinarErrorCode(RuntimeException ex) {
        if (ex instanceof com.app.telemetria.exception.MotoristaNotFoundException) {
            return ErrorCode.MOTORISTA_NOT_FOUND;
        }
        if (ex instanceof com.app.telemetria.exception.VeiculoNotFoundException) {
            return ErrorCode.VEICULO_NOT_FOUND;
        }
        if (ex instanceof com.app.telemetria.exception.RotaNotFoundException) {
            return ErrorCode.ROTA_NOT_FOUND;
        }
        if (ex instanceof com.app.telemetria.exception.ViagemNotFoundException) {
            return ErrorCode.VIAGEM_NOT_FOUND;
        }
        return ErrorCode.RESOURCE_NOT_FOUND;
    }

    // ================= CONFLITO (409) =================
    @ExceptionHandler({
        com.app.telemetria.exception.MotoristaDuplicateException.class,
        com.app.telemetria.exception.VeiculoDuplicateException.class,
        com.app.telemetria.exception.RotaDuplicateException.class
    })
    public ResponseEntity<ErrorResponse> handleDuplicateException(
            RuntimeException ex,
            HttpServletRequest request) {

        ErrorCode errorCode = determinarErrorCodeDuplicado(ex);

        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage(),
                errorCode.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    private ErrorCode determinarErrorCodeDuplicado(RuntimeException ex) {
        if (ex instanceof com.app.telemetria.exception.MotoristaDuplicateException) {
            return ErrorCode.MOTORISTA_DUPLICATE;
        }
        if (ex instanceof com.app.telemetria.exception.VeiculoDuplicateException) {
            return ErrorCode.VEICULO_DUPLICATE;
        }
        if (ex instanceof com.app.telemetria.exception.RotaDuplicateException) {
            return ErrorCode.ROTA_DUPLICATE;
        }
        return ErrorCode.DUPLICATE_RESOURCE;
    }

    // ================= ERROS DE NEGÓCIO (422) =================
    @ExceptionHandler({
        com.app.telemetria.exception.RotaValidationException.class,
        com.app.telemetria.exception.VeiculoIndisponivelException.class,
        com.app.telemetria.exception.MotoristaIndisponivelException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(
            RuntimeException ex,
            HttpServletRequest request) {

        ErrorCode errorCode = determinarErrorCodeBusiness(ex);

        ErrorResponse response = new ErrorResponse(
                errorCode.getCode(),
                ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage(),
                errorCode.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    private ErrorCode determinarErrorCodeBusiness(RuntimeException ex) {
        if (ex instanceof com.app.telemetria.exception.RotaValidationException) {
            return ErrorCode.ROTA_INVALID;
        }
        if (ex instanceof com.app.telemetria.exception.VeiculoIndisponivelException) {
            return ErrorCode.VEICULO_INDISPONIVEL;
        }
        if (ex instanceof com.app.telemetria.exception.MotoristaIndisponivelException) {
            return ErrorCode.MOTORISTA_INDISPONIVEL;
        }
        return ErrorCode.BUSINESS_ERROR;
    }

    // ================= INTEGRAÇÃO EXTERNA (424) =================
    @ExceptionHandler(com.app.telemetria.exception.WeatherApiException.class)
    public ResponseEntity<ErrorResponse> handleWeatherApiException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.WEATHER_API_ERROR.getCode(),
                ErrorCode.WEATHER_API_ERROR.getMessage(),
                ErrorCode.WEATHER_API_ERROR.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.WEATHER_API_ERROR.getHttpStatus())
                .body(response);
    }

    // ================= MUITAS REQUISIÇÕES (429) =================
    @ExceptionHandler(org.springframework.web.client.HttpClientErrorException.TooManyRequests.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequestsException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.TOO_MANY_REQUESTS.getCode(),
                ErrorCode.TOO_MANY_REQUESTS.getMessage(),
                ErrorCode.TOO_MANY_REQUESTS.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.TOO_MANY_REQUESTS.getHttpStatus())
                .body(response);
    }

    // ================= ERRO NO BANCO DE DADOS (500) =================
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.DATABASE_ERROR.getCode(),
                ErrorCode.DATABASE_ERROR.getMessage(),
                ErrorCode.DATABASE_ERROR.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.DATABASE_ERROR.getHttpStatus())
                .body(response);
    }

    // ================= ERRO NO KAFKA (500) =================
    @ExceptionHandler(org.springframework.kafka.core.KafkaProducerException.class)
    public ResponseEntity<ErrorResponse> handleKafkaProducerException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.KAFKA_PRODUCER_ERROR.getCode(),
                ErrorCode.KAFKA_PRODUCER_ERROR.getMessage(),
                ErrorCode.KAFKA_PRODUCER_ERROR.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.KAFKA_PRODUCER_ERROR.getHttpStatus())
                .body(response);
    }

    // ================= ERRO NO MQTT (500) - CORRIGIDO =================
    @ExceptionHandler({
        org.eclipse.paho.client.mqttv3.MqttException.class,
        org.eclipse.paho.client.mqttv3.MqttSecurityException.class,
        org.eclipse.paho.client.mqttv3.MqttPersistenceException.class
    })
    public ResponseEntity<ErrorResponse> handleMqttException(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.MQTT_CONNECTION_ERROR.getCode(),
                "Erro na comunicação MQTT: " + ex.getMessage(),
                ErrorCode.MQTT_CONNECTION_ERROR.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.MQTT_CONNECTION_ERROR.getHttpStatus())
                .body(response);
    }

    // ================= TIMEOUT (504) =================
    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                ErrorCode.GATEWAY_TIMEOUT.getCode(),
                ErrorCode.GATEWAY_TIMEOUT.getMessage(),
                ErrorCode.GATEWAY_TIMEOUT.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.GATEWAY_TIMEOUT.getHttpStatus())
                .body(response);
    }

    // ================= GENÉRICO (500) =================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        // Log do erro para debugging
        System.err.println("Erro não tratado: " + ex.getClass().getName() + " - " + ex.getMessage());
        ex.printStackTrace();

        ErrorResponse response = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage(),
                ErrorCode.INTERNAL_ERROR.getHttpStatus().value(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(response);
    }
}