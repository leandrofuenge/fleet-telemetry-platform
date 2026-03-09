package com.app.telemetria.entity;

import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore; // <-- ADICIONAR

@Entity
@Table(name = "veiculos")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String placa;

    private String modelo;
    private Double capacidadeCarga;

    // =========================================
    // RELACIONAMENTOS OTIMIZADOS
    // =========================================

    @JsonIgnore 
    @OneToMany(mappedBy = "veiculo", 
               fetch = FetchType.LAZY,
               cascade = {}) 
    private List<Telemetria> historicoTelemetria = new ArrayList<>();

    @JsonIgnore 
    @OneToMany(mappedBy = "veiculo", 
               fetch = FetchType.LAZY,
               cascade = {}) 
    private List<Rota> rotas = new ArrayList<>();

    // =========================================
    // MÉTODOS UTILITÁRIOS PARA MANIPULAR RELACIONAMENTOS
    // =========================================

    public void addRota(Rota rota) {
        rotas.add(rota);
        rota.setVeiculo(this);
    }

    // Remover uma rota do veículo
    public void removeRota(Rota rota) {
        rotas.remove(rota);
        rota.setVeiculo(null);
    }

    // Adicionar telemetria ao veículo
    public void addTelemetria(Telemetria telemetria) {
        historicoTelemetria.add(telemetria);
        telemetria.setVeiculo(this);
    }

    // =========================================
    // CONSTRUTORES
    // =========================================
    
    public Veiculo() {}

    // Construtor com campos principais
    public Veiculo(String placa, String modelo, Double capacidadeCarga) {
        this.placa = placa;
        this.modelo = modelo;
        this.capacidadeCarga = capacidadeCarga;
    }

    // =========================================
    // GETTERS E SETTERS
    // =========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Double getCapacidadeCarga() {
        return capacidadeCarga;
    }

    public void setCapacidadeCarga(Double capacidadeCarga) {
        this.capacidadeCarga = capacidadeCarga;
    }

    public List<Telemetria> getHistoricoTelemetria() {
        return historicoTelemetria;
    }

    public void setHistoricoTelemetria(List<Telemetria> historicoTelemetria) {
        this.historicoTelemetria = historicoTelemetria;
    }

    public List<Rota> getRotas() {
        return rotas;
    }

    public void setRotas(List<Rota> rotas) {
        this.rotas = rotas;
    }
    
    @Override
    public String toString() {
        return "Veiculo{" +
                "id=" + id +
                ", placa='" + placa + '\'' +
                ", modelo='" + modelo + '\'' +
                ", capacidadeCarga=" + capacidadeCarga +
                '}';
    }
}