package com.app.routing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "veiculos_cache", indexes = {
        @Index(name = "idx_vc_tenant", columnList = "tenant_id")
})
public class VeiculoCache {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "placa", nullable = false, length = 10)
    private String placa;

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "tipo_veiculo", nullable = false, length = 50)
    private String tipoVeiculo;

    @Column(name = "consumo_medio")
    private Double consumoMedio;

    @Column(name = "pbt_kg")
    private Double pbtKg;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @UpdateTimestamp
    @Column(name = "sincronizado_em")
    private LocalDateTime sincronizadoEm;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão (sem argumentos).
     * Inicializa os campos com os valores padrão definidos nos @Builder.Default
     * originais.
     */
    public VeiculoCache() {
        this.tipoVeiculo = "CAMINHAO_PESADO";
        this.ativo = true;
    }

    /**
     * Construtor privado com todos os campos.
     * Usado internamente pelo Builder.
     */
    private VeiculoCache(Long id, String uuid, Long tenantId, String placa,
            String modelo, String tipoVeiculo, Double consumoMedio,
            Double pbtKg, Boolean ativo, LocalDateTime sincronizadoEm) {
        this.id = id;
        this.uuid = uuid;
        this.tenantId = tenantId;
        this.placa = placa;
        this.modelo = modelo;
        this.tipoVeiculo = tipoVeiculo != null ? tipoVeiculo : "CAMINHAO_PESADO";
        this.consumoMedio = consumoMedio;
        this.pbtKg = pbtKg;
        this.ativo = ativo != null ? ativo : true;
        this.sincronizadoEm = sincronizadoEm;
    }

    // ==================== GETTERS E SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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

    public String getTipoVeiculo() {
        return tipoVeiculo;
    }

    public void setTipoVeiculo(String tipoVeiculo) {
        this.tipoVeiculo = tipoVeiculo;
    }

    public Double getConsumoMedio() {
        return consumoMedio;
    }

    public void setConsumoMedio(Double consumoMedio) {
        this.consumoMedio = consumoMedio;
    }

    public Double getPbtKg() {
        return pbtKg;
    }

    public void setPbtKg(Double pbtKg) {
        this.pbtKg = pbtKg;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getSincronizadoEm() {
        return sincronizadoEm;
    }

    public void setSincronizadoEm(LocalDateTime sincronizadoEm) {
        this.sincronizadoEm = sincronizadoEm;
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String uuid;
        private Long tenantId;
        private String placa;
        private String modelo;
        private String tipoVeiculo = "CAMINHAO_PESADO"; // valor padrão
        private Double consumoMedio;
        private Double pbtKg;
        private Boolean ativo = true; // valor padrão
        private LocalDateTime sincronizadoEm;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder placa(String placa) {
            this.placa = placa;
            return this;
        }

        public Builder modelo(String modelo) {
            this.modelo = modelo;
            return this;
        }

        public Builder tipoVeiculo(String tipoVeiculo) {
            this.tipoVeiculo = tipoVeiculo;
            return this;
        }

        public Builder consumoMedio(Double consumoMedio) {
            this.consumoMedio = consumoMedio;
            return this;
        }

        public Builder pbtKg(Double pbtKg) {
            this.pbtKg = pbtKg;
            return this;
        }

        public Builder ativo(Boolean ativo) {
            this.ativo = ativo;
            return this;
        }

        public Builder sincronizadoEm(LocalDateTime sincronizadoEm) {
            this.sincronizadoEm = sincronizadoEm;
            return this;
        }

        public VeiculoCache build() {
            return new VeiculoCache(
                    id, uuid, tenantId, placa,
                    modelo, tipoVeiculo, consumoMedio,
                    pbtKg, ativo, sincronizadoEm);
        }
    }
}