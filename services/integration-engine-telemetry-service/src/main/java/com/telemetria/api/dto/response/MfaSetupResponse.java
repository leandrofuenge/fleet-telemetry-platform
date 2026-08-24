package com.telemetria.api.dto.response;

public record MfaSetupResponse(String secret, String otpauthUri) {}
