package com.app.telemetria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.app.telemetria.util.PontoRotaConverter;

@Entity
@Table(name = "rotas")
public class Rota {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nome;
    
    private String origem;
    private String destino;
    private Double latitudeOrigem;
    private Double longitudeOrigem;
    private Double latitudeDestino;
    private Double longitudeDestino;
    private Double distanciaPrevista;
    private Integer tempoPrevisto;
    private Boolean ativa;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    
    @ManyToOne
    @JoinColumn(name = "veiculo_id")  
    private Veiculo veiculo;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Convert(converter = PontoRotaConverter.class)
    @Column(name = "pontos_rota", columnDefinition = "JSON")
    private List<PontoRota> pontosRota = new ArrayList<>();
    
    @Column(nullable = false)
    private String status; // "PLANEJADA", "EM_ANDAMENTO", "FINALIZADA", "CANCELADA"

    
    // ========= GETTERS E SETTERS =========
    
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
    
    public String getDestino() {
        return destino;
    }
    
    public void setDestino(String destino) {
        this.destino = destino;
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
    
    public Boolean getAtiva() {
        return ativa;
    }
    
    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }
    
    public LocalDateTime getDataInicio() {
        return dataInicio;
    }
    
    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }
    
    public LocalDateTime getDataFim() {
        return dataFim;
    }
    
    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }
    
    public Veiculo getVeiculo() {
        return veiculo;
    }
    
    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
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
    
    public List<PontoRota> getPontosRota() { return pontosRota; }
    
    public void setPontosRota(List<PontoRota> pontosRota) { this.pontosRota = pontosRota; }
    
 // Getters e Setters
    public String getStatus() { return status; }
    
    public void setStatus(String status) { this.status = status; }
    
}


