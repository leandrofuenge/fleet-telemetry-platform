package com.telemetria.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaConfirmRequest extends MfaSetupRequest {
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "O código MFA deve ter 6 dígitos")
    private String codigo;
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}
