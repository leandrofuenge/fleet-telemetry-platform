// src/main/java/com/telemetria/api/dto/response/ETAResponseDTO.java
package com.telemetria.api.dto.response;

import java.time.LocalDateTime;

/**
 * DTO para resposta de consulta de ETA (Tempo Estimado de Chegada)
 * Utilizado para retornar informações dinâmicas de previsão de chegada
 * 
 * @author Telemetria Team
 * @version 1.0
 */
public class ETAResponseDTO {
    
    private Long viagemId;
    private Long veiculoId;
    private String placaVeiculo;
    private String motoristaNome;
    
    private Double latitudeAtual;
    private Double longitudeAtual;
    private Double velocidadeAtualKmh;
    
    private Double distanciaRestanteKm;
    private Double minutosRestantes;
    private LocalDateTime etaCalculado;
    private LocalDateTime etaPrevistoOriginal;
    
    private Long atrasoMinutos;
    private String statusEta; // NORMAL, ATRASO_LEVE, ATRASO_MODERADO, ATRASO_CRITICO, INDETERMINADO
    private String mensagemStatus;
    
    private Boolean paradaNaoPrevistaDetectada;
    private Integer tempoParadoMinutos;
    
    private LocalDateTime ultimaAtualizacao;
    
    // =========================================
    // Construtores
    // =========================================
    
    public ETAResponseDTO() {
    }
    
    public ETAResponseDTO(Builder builder) {
        this.viagemId = builder.viagemId;
        this.veiculoId = builder.veiculoId;
        this.placaVeiculo = builder.placaVeiculo;
        this.motoristaNome = builder.motoristaNome;
        this.latitudeAtual = builder.latitudeAtual;
        this.longitudeAtual = builder.longitudeAtual;
        this.velocidadeAtualKmh = builder.velocidadeAtualKmh;
        this.distanciaRestanteKm = builder.distanciaRestanteKm;
        this.minutosRestantes = builder.minutosRestantes;
        this.etaCalculado = builder.etaCalculado;
        this.etaPrevistoOriginal = builder.etaPrevistoOriginal;
        this.atrasoMinutos = builder.atrasoMinutos;
        this.statusEta = builder.statusEta;
        this.mensagemStatus = builder.mensagemStatus;
        this.paradaNaoPrevistaDetectada = builder.paradaNaoPrevistaDetectada;
        this.tempoParadoMinutos = builder.tempoParadoMinutos;
        this.ultimaAtualizacao = builder.ultimaAtualizacao;
    }
    
    // =========================================
    // Builder Pattern
    // =========================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long viagemId;
        private Long veiculoId;
        private String placaVeiculo;
        private String motoristaNome;
        private Double latitudeAtual;
        private Double longitudeAtual;
        private Double velocidadeAtualKmh;
        private Double distanciaRestanteKm;
        private Double minutosRestantes;
        private LocalDateTime etaCalculado;
        private LocalDateTime etaPrevistoOriginal;
        private Long atrasoMinutos;
        private String statusEta;
        private String mensagemStatus;
        private Boolean paradaNaoPrevistaDetectada;
        private Integer tempoParadoMinutos;
        private LocalDateTime ultimaAtualizacao;
        
        private Builder() {
        }
        
        public Builder viagemId(Long viagemId) {
            this.viagemId = viagemId;
            return this;
        }
        
        public Builder veiculoId(Long veiculoId) {
            this.veiculoId = veiculoId;
            return this;
        }
        
        public Builder placaVeiculo(String placaVeiculo) {
            this.placaVeiculo = placaVeiculo;
            return this;
        }
        
        public Builder motoristaNome(String motoristaNome) {
            this.motoristaNome = motoristaNome;
            return this;
        }
        
        public Builder latitudeAtual(Double latitudeAtual) {
            this.latitudeAtual = latitudeAtual;
            return this;
        }
        
        public Builder longitudeAtual(Double longitudeAtual) {
            this.longitudeAtual = longitudeAtual;
            return this;
        }
        
        public Builder velocidadeAtualKmh(Double velocidadeAtualKmh) {
            this.velocidadeAtualKmh = velocidadeAtualKmh;
            return this;
        }
        
        public Builder distanciaRestanteKm(Double distanciaRestanteKm) {
            this.distanciaRestanteKm = distanciaRestanteKm;
            return this;
        }
        
        public Builder minutosRestantes(Double minutosRestantes) {
            this.minutosRestantes = minutosRestantes;
            return this;
        }
        
        public Builder etaCalculado(LocalDateTime etaCalculado) {
            this.etaCalculado = etaCalculado;
            return this;
        }
        
        public Builder etaPrevistoOriginal(LocalDateTime etaPrevistoOriginal) {
            this.etaPrevistoOriginal = etaPrevistoOriginal;
            return this;
        }
        
        public Builder atrasoMinutos(Long atrasoMinutos) {
            this.atrasoMinutos = atrasoMinutos;
            return this;
        }
        
        public Builder statusEta(String statusEta) {
            this.statusEta = statusEta;
            return this;
        }
        
        public Builder mensagemStatus(String mensagemStatus) {
            this.mensagemStatus = mensagemStatus;
            return this;
        }
        
        public Builder paradaNaoPrevistaDetectada(Boolean paradaNaoPrevistaDetectada) {
            this.paradaNaoPrevistaDetectada = paradaNaoPrevistaDetectada;
            return this;
        }
        
        public Builder tempoParadoMinutos(Integer tempoParadoMinutos) {
            this.tempoParadoMinutos = tempoParadoMinutos;
            return this;
        }
        
        public Builder ultimaAtualizacao(LocalDateTime ultimaAtualizacao) {
            this.ultimaAtualizacao = ultimaAtualizacao;
            return this;
        }
        
        public ETAResponseDTO build() {
            return new ETAResponseDTO(this);
        }
    }
    
    // =========================================
    // Getters e Setters
    // =========================================
    
    public Long getViagemId() {
        return viagemId;
    }
    
    public void setViagemId(Long viagemId) {
        this.viagemId = viagemId;
    }
    
    public Long getVeiculoId() {
        return veiculoId;
    }
    
    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }
    
    public String getPlacaVeiculo() {
        return placaVeiculo;
    }
    
    public void setPlacaVeiculo(String placaVeiculo) {
        this.placaVeiculo = placaVeiculo;
    }
    
    public String getMotoristaNome() {
        return motoristaNome;
    }
    
    public void setMotoristaNome(String motoristaNome) {
        this.motoristaNome = motoristaNome;
    }
    
    public Double getLatitudeAtual() {
        return latitudeAtual;
    }
    
    public void setLatitudeAtual(Double latitudeAtual) {
        this.latitudeAtual = latitudeAtual;
    }
    
    public Double getLongitudeAtual() {
        return longitudeAtual;
    }
    
    public void setLongitudeAtual(Double longitudeAtual) {
        this.longitudeAtual = longitudeAtual;
    }
    
    public Double getVelocidadeAtualKmh() {
        return velocidadeAtualKmh;
    }
    
    public void setVelocidadeAtualKmh(Double velocidadeAtualKmh) {
        this.velocidadeAtualKmh = velocidadeAtualKmh;
    }
    
    public Double getDistanciaRestanteKm() {
        return distanciaRestanteKm;
    }
    
    public void setDistanciaRestanteKm(Double distanciaRestanteKm) {
        this.distanciaRestanteKm = distanciaRestanteKm;
    }
    
    public Double getMinutosRestantes() {
        return minutosRestantes;
    }
    
    public void setMinutosRestantes(Double minutosRestantes) {
        this.minutosRestantes = minutosRestantes;
    }
    
    public LocalDateTime getEtaCalculado() {
        return etaCalculado;
    }
    
    public void setEtaCalculado(LocalDateTime etaCalculado) {
        this.etaCalculado = etaCalculado;
    }
    
    public LocalDateTime getEtaPrevistoOriginal() {
        return etaPrevistoOriginal;
    }
    
    public void setEtaPrevistoOriginal(LocalDateTime etaPrevistoOriginal) {
        this.etaPrevistoOriginal = etaPrevistoOriginal;
    }
    
    public Long getAtrasoMinutos() {
        return atrasoMinutos;
    }
    
    public void setAtrasoMinutos(Long atrasoMinutos) {
        this.atrasoMinutos = atrasoMinutos;
    }
    
    public String getStatusEta() {
        return statusEta;
    }
    
    public void setStatusEta(String statusEta) {
        this.statusEta = statusEta;
    }
    
    public String getMensagemStatus() {
        return mensagemStatus;
    }
    
    public void setMensagemStatus(String mensagemStatus) {
        this.mensagemStatus = mensagemStatus;
    }
    
    public Boolean getParadaNaoPrevistaDetectada() {
        return paradaNaoPrevistaDetectada;
    }
    
    public void setParadaNaoPrevistaDetectada(Boolean paradaNaoPrevistaDetectada) {
        this.paradaNaoPrevistaDetectada = paradaNaoPrevistaDetectada;
    }
    
    public Integer getTempoParadoMinutos() {
        return tempoParadoMinutos;
    }
    
    public void setTempoParadoMinutos(Integer tempoParadoMinutos) {
        this.tempoParadoMinutos = tempoParadoMinutos;
    }
    
    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }
    
    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }
    
    // =========================================
    // Métodos utilitários
    // =========================================
    
    @Override
    public String toString() {
        return "ETAResponseDTO{" +
                "viagemId=" + viagemId +
                ", veiculoId=" + veiculoId +
                ", placaVeiculo='" + placaVeiculo + '\'' +
                ", motoristaNome='" + motoristaNome + '\'' +
                ", latitudeAtual=" + latitudeAtual +
                ", longitudeAtual=" + longitudeAtual +
                ", velocidadeAtualKmh=" + velocidadeAtualKmh +
                ", distanciaRestanteKm=" + distanciaRestanteKm +
                ", minutosRestantes=" + minutosRestantes +
                ", etaCalculado=" + etaCalculado +
                ", etaPrevistoOriginal=" + etaPrevistoOriginal +
                ", atrasoMinutos=" + atrasoMinutos +
                ", statusEta='" + statusEta + '\'' +
                ", mensagemStatus='" + mensagemStatus + '\'' +
                ", paradaNaoPrevistaDetectada=" + paradaNaoPrevistaDetectada +
                ", tempoParadoMinutos=" + tempoParadoMinutos +
                ", ultimaAtualizacao=" + ultimaAtualizacao +
                '}';
    }
    
    /**
     * Verifica se há atraso na viagem
     * 
     * @return true se atrasoMinutos > 0
     */
    public boolean hasAtraso() {
        return atrasoMinutos != null && atrasoMinutos > 0;
    }
    
    /**
     * Verifica se o ETA está indeterminado
     * 
     * @return true se statusEta for INDETERMINADO
     */
    public boolean isIndeterminado() {
        return "INDETERMINADO".equals(statusEta);
    }
    
    /**
     * Verifica se há parada não prevista
     * 
     * @return true se paradaNaoPrevistaDetectada for true
     */
    public boolean hasParadaNaoPrevista() {
        return Boolean.TRUE.equals(paradaNaoPrevistaDetectada);
    }
}