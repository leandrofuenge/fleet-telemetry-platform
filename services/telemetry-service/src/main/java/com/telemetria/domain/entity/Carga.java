package com.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "cargas", indexes = {
        @Index(name = "idx_car_cliente", columnList = "cliente_id"),
        @Index(name = "idx_car_tipo", columnList = "tipo")
})
public class Carga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "peso_kg")
    private Double peso;

    @Column(length = 50)
    private String tipo; // GERAL, REFRIGERADA, CONGELADA, PERIGOSA, FRAGIL, GRANEL, LIQUIDO, VALORES

    @Column(name = "volume_m3")
    private Double volumeM3;

    // Documentação fiscal
    @Column(name = "nfe_chave", length = 50)
    private String nfeChave;

    @Column(name = "cte_chave", length = 50)
    private String cteChave;

    // ── Relacionamento com Cliente (embarcador/contratante) ────

    /**
     * Muitas cargas pertencem a um cliente.
     * Lado N do N:1 com Cliente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // ── Relacionamento com Viagens ─────────────────────────────

    /**
     * Uma carga pode ser transportada em várias viagens
     * (ex: carga partida em múltiplas entregas).
     */
    @OneToMany(mappedBy = "carga", fetch = FetchType.LAZY, cascade = {})
    private List<Viagem> viagens = new ArrayList<>();

    // ── Auditoria ──────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Construtores ───────────────────────────────────────────

    public Carga() {
    }

    public Carga(String descricao, Double peso, Cliente cliente) {
        this.descricao = descricao;
        this.peso = peso;
        this.cliente = cliente;
    }

    // ── Getters e Setters ──────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getVolumeM3() {
        return volumeM3;
    }

    public void setVolumeM3(Double volumeM3) {
        this.volumeM3 = volumeM3;
    }

    public String getNfeChave() {
        return nfeChave;
    }

    public void setNfeChave(String nfeChave) {
        this.nfeChave = nfeChave;
    }

    public String getCteChave() {
        return cteChave;
    }

    public void setCteChave(String cteChave) {
        this.cteChave = cteChave;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<Viagem> getViagens() {
        return viagens;
    }

    public void setViagens(List<Viagem> viagens) {
        this.viagens = viagens;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public String toString() {
        return "Carga{id=" + id + ", descricao='" + descricao + "', peso=" + peso + "}";
    }
}
