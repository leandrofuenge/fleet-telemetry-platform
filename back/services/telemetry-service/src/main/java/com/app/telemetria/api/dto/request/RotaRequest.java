package com.app.telemetria.api.dto.request;

public class RotaRequest {
    
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

    // Construtores
    public RotaRequest() {}

    public RotaRequest(String nome, String origem, Double latitudeOrigem, Double longitudeOrigem,
                      String destino, Double latitudeDestino, Double longitudeDestino,
                      Double distanciaPrevista, Integer tempoPrevisto, String status, Boolean ativa) {
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
    }

    // Getters e Setters
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

    @Override
    public String toString() {
        return "RotaRequest{" +
                "nome='" + nome + '\'' +
                ", origem='" + origem + '\'' +
                ", destino='" + destino + '\'' +
                ", distanciaPrevista=" + distanciaPrevista +
                ", status='" + status + '\'' +
                '}';
    }
}