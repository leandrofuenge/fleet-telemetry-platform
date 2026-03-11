package com.app.telemetria.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class PontoRota {

    private Double latitude;
    private Double longitude;
    private Integer ordem;
    private String endereco;

    public PontoRota() {
    }

    public PontoRota(Double latitude, Double longitude, Integer ordem) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.ordem = ordem;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }
}
