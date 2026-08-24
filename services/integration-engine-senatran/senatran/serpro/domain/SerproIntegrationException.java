package com.telemetria.integration.senatran.serpro.domain;

public class SerproIntegrationException extends RuntimeException {
    public SerproIntegrationException(String message) { super(message); }
    public SerproIntegrationException(String message, Throwable cause) { super(message, cause); }
}
