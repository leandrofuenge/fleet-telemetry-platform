package com.telemetria.api.dto.response;

import java.time.LocalDateTime;

public class RotaResponse {
    
    private Long id;
    private String nome;
    private String origem;
    private Double latitudeOrigem;
    private Double longitudeOrigem;
    private String destino;
    private Double latitudeDestino;
    private Double longitudeDestino;
    private Double distanciaPrevista;
    private Integer tempoPrevisto;
    private String status;
    private Boolean ativa;
    private Long veiculoId;
    private Long motoristaId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RotaResponse(Long id, String nome, String origem, Double latitudeOrigem, Double longitudeOrigem,
                       String destino, Double latitudeDestino, Double longitudeDestino,
                       Double distanciaPrevista, Integer tempoPrevisto, String status, Boolean ativa,
                       Long veiculoId, Long motoristaId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.nome = nome;
        this.origem = origem;
        this.latitudeOrigem = latitudeOrigem;
        this.longitudeOrigem = longitudeOrigem;
        this.destino = destino;
        this.latitudeDestino = latitudeDestino;
        this.longitudeDestino = longitudeDestino;
        this.distanciaPrevista = distanciaPrevista;
        this.tempoPrevisto = tempoPrevisto;
        this.status = status;
        this.ativa = ativa;
        this.veiculoId = veiculoId;
        this.motoristaId = motoristaId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public Double getLatitudeOrigem() {
        return latitudeOrigem;
    }

    public void setLatitudeOrigem(Double latitudeOrigem) {
        this.latitudeOrigem = latitudeOrigem;
    }

    public Double getLongitudeOrigem() {
        return longitudeOrigem;
    }

    public void setLongitudeOrigem(Double longitudeOrigem) {
        this.longitudeOrigem = longitudeOrigem;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public Double getLatitudeDestino() {
        return latitudeDestino;
    }

    public void setLatitudeDestino(Double latitudeDestino) {
        this.latitudeDestino = latitudeDestino;
    }

    public Double getLongitudeDestino() {
        return longitudeDestino;
    }

    public void setLongitudeDestino(Double longitudeDestino) {
        this.longitudeDestino = longitudeDestino;
    }

    public Double getDistanciaPrevista() {
        return distanciaPrevista;
    }

    public void setDistanciaPrevista(Double distanciaPrevista) {
        this.distanciaPrevista = distanciaPrevista;
    }

    public Integer getTempoPrevisto() {
        return tempoPrevisto;
    }

    public void setTempoPrevisto(Integer tempoPrevisto) {
        this.tempoPrevisto = tempoPrevisto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public Long getMotoristaId() {
        return motoristaId;
    }

    public void setMotoristaId(Long motoristaId) {
        this.motoristaId = motoristaId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}