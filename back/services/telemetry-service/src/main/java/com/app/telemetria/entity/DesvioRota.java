package com.app.telemetria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "desvios_rota")
public class DesvioRota {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "rota_id", nullable = false)
    private Rota rota;
    
    @ManyToOne
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;
    
    private Double latitudeDesvio;
    private Double longitudeDesvio;
    private Double distanciaDesvio; // Em metros
    private LocalDateTime dataHoraDesvio;
    private Boolean resolvido;
    private LocalDateTime dataHoraRetorno;

    // Getters e Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Rota getRota() {
        return rota;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public Double getLatitudeDesvio() {
        return latitudeDesvio;
    }

    public void setLatitudeDesvio(Double latitudeDesvio) {
        this.latitudeDesvio = latitudeDesvio;
    }

    public Double getLongitudeDesvio() {
        return longitudeDesvio;
    }

    public void setLongitudeDesvio(Double longitudeDesvio) {
        this.longitudeDesvio = longitudeDesvio;
    }

    public Double getDistanciaDesvio() {
        return distanciaDesvio;
    }

    public void setDistanciaDesvio(Double distanciaDesvio) {
        this.distanciaDesvio = distanciaDesvio;
    }

    public LocalDateTime getDataHoraDesvio() {
        return dataHoraDesvio;
    }

    public void setDataHoraDesvio(LocalDateTime dataHoraDesvio) {
        this.dataHoraDesvio = dataHoraDesvio;
    }

    public Boolean getResolvido() {
        return resolvido;
    }

    public void setResolvido(Boolean resolvido) {
        this.resolvido = resolvido;
    }

    public LocalDateTime getDataHoraRetorno() {
        return dataHoraRetorno;
    }

    public void setDataHoraRetorno(LocalDateTime dataHoraRetorno) {
        this.dataHoraRetorno = dataHoraRetorno;
    }
}
