package com.app.telemetria.api.dto.response;

import java.time.LocalDateTime;

public class VeiculoCacheDTO {
    
    private Long id;
    private String placa;
    private String marca;
    private String modelo;
    private Integer anoFabricacao;
    private Boolean ativo;
    private Double capacidadeCarga; // Alterado de BigDecimal para Double
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    
    // Dados do Cliente (achatados)
    private Long clienteId;
    private String clienteNome;
    private String clienteDocumento;
    private Long clienteTenantId; // Adicionado campo para tenantId
    
    // Dados do Motorista Atual (achatados)
    private Long motoristaId;
    private String motoristaNome;
    private String motoristaCpf;
    private String motoristaCnh;
    
    // Construtor padrão
    public VeiculoCacheDTO() {}
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    
    public Integer getAnoFabricacao() { return anoFabricacao; }
    public void setAnoFabricacao(Integer anoFabricacao) { this.anoFabricacao = anoFabricacao; }
    
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    
    public Double getCapacidadeCarga() { return capacidadeCarga; }
    public void setCapacidadeCarga(Double capacidadeCarga) { this.capacidadeCarga = capacidadeCarga; }
    
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    
    public String getClienteDocumento() { return clienteDocumento; }
    public void setClienteDocumento(String clienteDocumento) { this.clienteDocumento = clienteDocumento; }
    
    public Long getClienteTenantId() { return clienteTenantId; }
    public void setClienteTenantId(Long clienteTenantId) { this.clienteTenantId = clienteTenantId; }
    
    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long motoristaId) { this.motoristaId = motoristaId; }
    
    public String getMotoristaNome() { return motoristaNome; }
    public void setMotoristaNome(String motoristaNome) { this.motoristaNome = motoristaNome; }
    
    public String getMotoristaCpf() { return motoristaCpf; }
    public void setMotoristaCpf(String motoristaCpf) { this.motoristaCpf = motoristaCpf; }
    
    public String getMotoristaCnh() { return motoristaCnh; }
    public void setMotoristaCnh(String motoristaCnh) { this.motoristaCnh = motoristaCnh; }
}