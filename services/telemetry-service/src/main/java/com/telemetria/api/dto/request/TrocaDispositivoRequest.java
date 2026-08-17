package com.telemetria.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class TrocaDispositivoRequest {
    @NotBlank private String deviceId;
    @NotNull @PositiveOrZero private Double odometroAtualKm;
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Double getOdometroAtualKm() { return odometroAtualKm; }
    public void setOdometroAtualKm(Double odometroAtualKm) { this.odometroAtualKm = odometroAtualKm; }
}
