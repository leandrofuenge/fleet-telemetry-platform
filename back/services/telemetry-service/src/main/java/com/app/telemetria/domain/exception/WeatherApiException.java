package com.app.telemetria.domain.exception;

public class WeatherApiException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public WeatherApiException(String message) {
        super(message);
    }
    
    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}