package com.app.telemetria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "geocoding_cache", 
       indexes = {
           @Index(name = "idx_coordenadas", columnList = "latitude,longitude"),
           @Index(name = "idx_data_consulta", columnList = "dataConsulta")
       })
public class GeocodingCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Double latitude;
    
    @Column(nullable = false)
    private Double longitude;
    
    @Column(nullable = false)
    private Boolean isUrbano;
    
    private String nomeLocal;
    private String tipoLocal; // city, town, village, etc.
    private String pais;
    private String estado;
    private String cidade;
    
    @Column(name = "data_consulta")
    private LocalDateTime dataConsulta;
    
    @Column(name = "precisao_metros")
    private Integer precisaoMetros;

    // ======================
    // Getters e Setters
    // ======================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getIsUrbano() {
        return isUrbano;
    }

    public void setIsUrbano(Boolean isUrbano) {
        this.isUrbano = isUrbano;
    }

    public String getNomeLocal() {
        return nomeLocal;
    }

    public void setNomeLocal(String nomeLocal) {
        this.nomeLocal = nomeLocal;
    }

    public String getTipoLocal() {
        return tipoLocal;
    }

    public void setTipoLocal(String tipoLocal) {
        this.tipoLocal = tipoLocal;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public LocalDateTime getDataConsulta() {
        return dataConsulta;
    }

    public void setDataConsulta(LocalDateTime dataConsulta) {
        this.dataConsulta = dataConsulta;
    }

    public Integer getPrecisaoMetros() {
        return precisaoMetros;
    }

    public void setPrecisaoMetros(Integer precisaoMetros) {
        this.precisaoMetros = precisaoMetros;
    }
}