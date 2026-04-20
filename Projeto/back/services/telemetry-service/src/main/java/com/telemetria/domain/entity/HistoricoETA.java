// src/main/java/com/telemetria/domain/entity/HistoricoETA.java
package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "historico_eta", indexes = {
    @Index(name = "idx_eta_viagem", columnList = "viagem_id"),
    @Index(name = "idx_eta_data", columnList = "data_calculo"),
    @Index(name = "idx_eta_veiculo", columnList = "veiculo_id")
})
public class HistoricoETA {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "viagem_id", nullable = false)
    private Long viagemId;
    
    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;
    
    @Column(name = "latitude_atual")
    private Double latitudeAtual;
    
    @Column(name = "longitude_atual")
    private Double longitudeAtual;
    
    @Column(name = "minutos_restantes")
    private Double minutosRestantes;
    
    @Column(name = "distancia_restante_km")
    private Double distanciaRestanteKm;
    
    @Column(name = "eta_previsto")
    private LocalDateTime etaPrevisto;
    
    @Column(name = "eta_calculado")
    private LocalDateTime etaCalculado;
    
    @Column(name = "atraso_minutos")
    private Long atrasoMinutos;
    
    @Column(name = "status_eta", length = 20)
    private String statusEta; // NORMAL, ATRASO_LEVE, ATRASO_MODERADO, ATRASO_CRITICO, INDETERMINADO
    
    @Column(name = "motivo_indeterminado", length = 255)
    private String motivoIndeterminado;
    
    @Column(name = "velocidade_atual_kmh")
    private Double velocidadeAtualKmh;
    
    @Column(name = "tempo_parado_minutos")
    private Integer tempoParadoMinutos;
    
    @Column(name = "parada_prevista", nullable = false)
    private Boolean paradaPrevista;
    
    @Column(name = "data_calculo", nullable = false)
    private LocalDateTime dataCalculo;
    
    @Column(name = "notificacao_enviada_30min", nullable = false)
    private Boolean notificacaoEnviada30min;
    
    @Column(name = "notificacao_enviada_60min", nullable = false)
    private Boolean notificacaoEnviada60min;
    
    // =========================================
    // Construtores
    // =========================================
    
    public HistoricoETA() {
        this.paradaPrevista = false;
        this.notificacaoEnviada30min = false;
        this.notificacaoEnviada60min = false;
    }
    
    public HistoricoETA(Builder builder) {
        this.id = builder.id;
        this.viagemId = builder.viagemId;
        this.veiculoId = builder.veiculoId;
        this.latitudeAtual = builder.latitudeAtual;
        this.longitudeAtual = builder.longitudeAtual;
        this.minutosRestantes = builder.minutosRestantes;
        this.distanciaRestanteKm = builder.distanciaRestanteKm;
        this.etaPrevisto = builder.etaPrevisto;
        this.etaCalculado = builder.etaCalculado;
        this.atrasoMinutos = builder.atrasoMinutos;
        this.statusEta = builder.statusEta;
        this.motivoIndeterminado = builder.motivoIndeterminado;
        this.velocidadeAtualKmh = builder.velocidadeAtualKmh;
        this.tempoParadoMinutos = builder.tempoParadoMinutos;
        this.paradaPrevista = builder.paradaPrevista;
        this.dataCalculo = builder.dataCalculo;
        this.notificacaoEnviada30min = builder.notificacaoEnviada30min;
        this.notificacaoEnviada60min = builder.notificacaoEnviada60min;
    }
    
    // =========================================
    // Builder Pattern
    // =========================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long id;
        private Long viagemId;
        private Long veiculoId;
        private Double latitudeAtual;
        private Double longitudeAtual;
        private Double minutosRestantes;
        private Double distanciaRestanteKm;
        private LocalDateTime etaPrevisto;
        private LocalDateTime etaCalculado;
        private Long atrasoMinutos;
        private String statusEta;
        private String motivoIndeterminado;
        private Double velocidadeAtualKmh;
        private Integer tempoParadoMinutos;
        private Boolean paradaPrevista = false;
        private LocalDateTime dataCalculo;
        private Boolean notificacaoEnviada30min = false;
        private Boolean notificacaoEnviada60min = false;
        
        private Builder() {
        }
        
        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        
        public Builder viagemId(Long viagemId) {
            this.viagemId = viagemId;
            return this;
        }
        
        public Builder veiculoId(Long veiculoId) {
            this.veiculoId = veiculoId;
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
        
        public Builder minutosRestantes(Double minutosRestantes) {
            this.minutosRestantes = minutosRestantes;
            return this;
        }
        
        public Builder distanciaRestanteKm(Double distanciaRestanteKm) {
            this.distanciaRestanteKm = distanciaRestanteKm;
            return this;
        }
        
        public Builder etaPrevisto(LocalDateTime etaPrevisto) {
            this.etaPrevisto = etaPrevisto;
            return this;
        }
        
        public Builder etaCalculado(LocalDateTime etaCalculado) {
            this.etaCalculado = etaCalculado;
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
        
        public Builder motivoIndeterminado(String motivoIndeterminado) {
            this.motivoIndeterminado = motivoIndeterminado;
            return this;
        }
        
        public Builder velocidadeAtualKmh(Double velocidadeAtualKmh) {
            this.velocidadeAtualKmh = velocidadeAtualKmh;
            return this;
        }
        
        public Builder tempoParadoMinutos(Integer tempoParadoMinutos) {
            this.tempoParadoMinutos = tempoParadoMinutos;
            return this;
        }
        
        public Builder paradaPrevista(Boolean paradaPrevista) {
            this.paradaPrevista = paradaPrevista;
            return this;
        }
        
        public Builder dataCalculo(LocalDateTime dataCalculo) {
            this.dataCalculo = dataCalculo;
            return this;
        }
        
        public Builder notificacaoEnviada30min(Boolean notificacaoEnviada30min) {
            this.notificacaoEnviada30min = notificacaoEnviada30min;
            return this;
        }
        
        public Builder notificacaoEnviada60min(Boolean notificacaoEnviada60min) {
            this.notificacaoEnviada60min = notificacaoEnviada60min;
            return this;
        }
        
        public HistoricoETA build() {
            return new HistoricoETA(this);
        }
    }
    
    // =========================================
    // Getters
    // =========================================
    
    public Long getId() {
        return id;
    }
    
    public Long getViagemId() {
        return viagemId;
    }
    
    public Long getVeiculoId() {
        return veiculoId;
    }
    
    public Double getLatitudeAtual() {
        return latitudeAtual;
    }
    
    public Double getLongitudeAtual() {
        return longitudeAtual;
    }
    
    public Double getMinutosRestantes() {
        return minutosRestantes;
    }
    
    public Double getDistanciaRestanteKm() {
        return distanciaRestanteKm;
    }
    
    public LocalDateTime getEtaPrevisto() {
        return etaPrevisto;
    }
    
    public LocalDateTime getEtaCalculado() {
        return etaCalculado;
    }
    
    public Long getAtrasoMinutos() {
        return atrasoMinutos;
    }
    
    public String getStatusEta() {
        return statusEta;
    }
    
    public String getMotivoIndeterminado() {
        return motivoIndeterminado;
    }
    
    public Double getVelocidadeAtualKmh() {
        return velocidadeAtualKmh;
    }
    
    public Integer getTempoParadoMinutos() {
        return tempoParadoMinutos;
    }
    
    public Boolean getParadaPrevista() {
        return paradaPrevista;
    }
    
    public LocalDateTime getDataCalculo() {
        return dataCalculo;
    }
    
    public Boolean getNotificacaoEnviada30min() {
        return notificacaoEnviada30min;
    }
    
    public Boolean getNotificacaoEnviada60min() {
        return notificacaoEnviada60min;
    }
    
    // =========================================
    // Setters
    // =========================================
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setViagemId(Long viagemId) {
        this.viagemId = viagemId;
    }
    
    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }
    
    public void setLatitudeAtual(Double latitudeAtual) {
        this.latitudeAtual = latitudeAtual;
    }
    
    public void setLongitudeAtual(Double longitudeAtual) {
        this.longitudeAtual = longitudeAtual;
    }
    
    public void setMinutosRestantes(Double minutosRestantes) {
        this.minutosRestantes = minutosRestantes;
    }
    
    public void setDistanciaRestanteKm(Double distanciaRestanteKm) {
        this.distanciaRestanteKm = distanciaRestanteKm;
    }
    
    public void setEtaPrevisto(LocalDateTime etaPrevisto) {
        this.etaPrevisto = etaPrevisto;
    }
    
    public void setEtaCalculado(LocalDateTime etaCalculado) {
        this.etaCalculado = etaCalculado;
    }
    
    public void setAtrasoMinutos(Long atrasoMinutos) {
        this.atrasoMinutos = atrasoMinutos;
    }
    
    public void setStatusEta(String statusEta) {
        this.statusEta = statusEta;
    }
    
    public void setMotivoIndeterminado(String motivoIndeterminado) {
        this.motivoIndeterminado = motivoIndeterminado;
    }
    
    public void setVelocidadeAtualKmh(Double velocidadeAtualKmh) {
        this.velocidadeAtualKmh = velocidadeAtualKmh;
    }
    
    public void setTempoParadoMinutos(Integer tempoParadoMinutos) {
        this.tempoParadoMinutos = tempoParadoMinutos;
    }
    
    public void setParadaPrevista(Boolean paradaPrevista) {
        this.paradaPrevista = paradaPrevista;
    }
    
    public void setDataCalculo(LocalDateTime dataCalculo) {
        this.dataCalculo = dataCalculo;
    }
    
    public void setNotificacaoEnviada30min(Boolean notificacaoEnviada30min) {
        this.notificacaoEnviada30min = notificacaoEnviada30min;
    }
    
    public void setNotificacaoEnviada60min(Boolean notificacaoEnviada60min) {
        this.notificacaoEnviada60min = notificacaoEnviada60min;
    }
    
    // =========================================
    // Métodos utilitários
    // =========================================
    
    @Override
    public String toString() {
        return "HistoricoETA{" +
                "id=" + id +
                ", viagemId=" + viagemId +
                ", veiculoId=" + veiculoId +
                ", latitudeAtual=" + latitudeAtual +
                ", longitudeAtual=" + longitudeAtual +
                ", minutosRestantes=" + minutosRestantes +
                ", distanciaRestanteKm=" + distanciaRestanteKm +
                ", etaPrevisto=" + etaPrevisto +
                ", etaCalculado=" + etaCalculado +
                ", atrasoMinutos=" + atrasoMinutos +
                ", statusEta='" + statusEta + '\'' +
                ", motivoIndeterminado='" + motivoIndeterminado + '\'' +
                ", velocidadeAtualKmh=" + velocidadeAtualKmh +
                ", tempoParadoMinutos=" + tempoParadoMinutos +
                ", paradaPrevista=" + paradaPrevista +
                ", dataCalculo=" + dataCalculo +
                ", notificacaoEnviada30min=" + notificacaoEnviada30min +
                ", notificacaoEnviada60min=" + notificacaoEnviada60min +
                '}';
    }
    
    /**
     * Verifica se o ETA está indeterminado
     */
    public boolean isIndeterminado() {
        return "INDETERMINADO".equals(statusEta);
    }
    
    /**
     * Verifica se há atraso
     */
    public boolean hasAtraso() {
        return atrasoMinutos != null && atrasoMinutos > 0;
    }
    
    /**
     * Verifica se é necessário enviar notificação de 30 minutos
     */
    public boolean needsNotificacao30min() {
        return !notificacaoEnviada30min && atrasoMinutos != null && atrasoMinutos >= 30;
    }
    
    /**
     * Verifica se é necessário enviar notificação de 60 minutos
     */
    public boolean needsNotificacao60min() {
        return !notificacaoEnviada60min && atrasoMinutos != null && atrasoMinutos >= 60;
    }
}