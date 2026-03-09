package com.app.telemetria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "viagens")
public class Viagem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;
    
    @ManyToOne
    @JoinColumn(name = "motorista_id")
    private Motorista motorista;
    
    @ManyToOne
    @JoinColumn(name = "carga_id")
    private Carga carga;
    
    @ManyToOne
    @JoinColumn(name = "rota_id")
    private Rota rota;
    
    @Column(name = "data_saida")
    private LocalDateTime dataSaida;
    
    @Column(name = "data_chegada_prevista")
    private LocalDateTime dataChegadaPrevista;
    
    @Column(name = "data_chegada_real")
    private LocalDateTime dataChegadaReal;
    
    @Column(name = "data_inicio") 
    private LocalDateTime dataInicio;
    
    private String status; // PLANEJADA, EM_ANDAMENTO, FINALIZADA, CANCELADA
    
    @Column(columnDefinition = "TEXT")
    private String observacoes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // ============= GETTERS E SETTERS =============
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    
    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }
    
    public Carga getCarga() { return carga; }
    public void setCarga(Carga carga) { this.carga = carga; }
    
    public Rota getRota() { return rota; }  
    public void setRota(Rota rota) { this.rota = rota; }
    
    public LocalDateTime getDataSaida() { return dataSaida; }
    public void setDataSaida(LocalDateTime dataSaida) { this.dataSaida = dataSaida; }
    
    public LocalDateTime getDataChegadaPrevista() { return dataChegadaPrevista; }
    public void setDataChegadaPrevista(LocalDateTime dataChegadaPrevista) { this.dataChegadaPrevista = dataChegadaPrevista; }
    
    public LocalDateTime getDataChegadaReal() { return dataChegadaReal; } 
    public void setDataChegadaReal(LocalDateTime dataChegadaReal) { this.dataChegadaReal = dataChegadaReal; }
    
    public LocalDateTime getDataInicio() { return dataInicio; }  
    public void setDataInicio(LocalDateTime dataInicio) { this.dataInicio = dataInicio; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getObservacoes() { return observacoes; } 
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}