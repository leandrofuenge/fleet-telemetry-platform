package com.app.routing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "motoristas_cache", indexes = {
        @Index(name = "idx_mc_tenant", columnList = "tenant_id")
})
public class MotoristaCache {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "cpf", nullable = false, length = 14)
    private String cpf;

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
    public MotoristaCache() {
        this.ativo = true;
    }

    /**
     * Construtor com todos os campos.
     * Usado internamente pelo Builder.
     */
    private MotoristaCache(Long id, String uuid, Long tenantId, String nome, String cpf,
            Boolean ativo, LocalDateTime sincronizadoEm) {
        this.id = id;
        this.uuid = uuid;
        this.tenantId = tenantId;
        this.nome = nome;
        this.cpf = cpf;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
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
        private String nome;
        private String cpf;
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

        public Builder nome(String nome) {
            this.nome = nome;
            return this;
        }

        public Builder cpf(String cpf) {
            this.cpf = cpf;
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

        public MotoristaCache build() {
            return new MotoristaCache(id, uuid, tenantId, nome, cpf, ativo, sincronizadoEm);
        }
    }
}