package com.app.telemetria.dto;

public class VeiculoDTO {

    private Long id;
    private String placa;
    private String modelo;
    private Double capacidadeCarga;

    public VeiculoDTO() {
    }

    public VeiculoDTO(Long id, String placa, String modelo, Double capacidadeCarga) {
        this.id = id;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidadeCarga = capacidadeCarga;
    }

    public Long getId() {
        return id;
    }

    public String getPlaca() {
        return placa;
    }

    public String getModelo() {
        return modelo;
    }

    public Double getCapacidadeCarga() {
        return capacidadeCarga;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public void setCapacidadeCarga(Double capacidadeCarga) {
        this.capacidadeCarga = capacidadeCarga;
    }
}
