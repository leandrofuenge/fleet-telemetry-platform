package com.app.telemetria.exception;

public class WeatherApiException extends RuntimeException {
    public WeatherApiException(String message) { super(message); }
}