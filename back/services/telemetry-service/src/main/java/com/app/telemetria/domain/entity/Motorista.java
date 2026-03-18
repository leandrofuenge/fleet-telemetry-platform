package com.app.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "motoristas", indexes = {
        @Index(name = "idx_mot_cpf", columnList = "cpf"),
        @Index(name = "idx_mot_cnh", columnList = "cnh"),
        @Index(name = "idx_mot_ativo", columnList = "ativo")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Motorista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @NotBlank(message = "CNH é obrigatória")
    @Column(nullable = false, unique = true, length = 20)
    private String cnh;

    @NotBlank(message = "Categoria da CNH é obrigatória")
    @Column(name = "categoria_cnh", nullable = false, length = 5)
    private String categoriaCnh;

    @Column(unique = true, length = 200)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    // ── Relacionamentos ────────────────────────────────────────

    /**
     * Um motorista pode ter várias viagens ao longo do tempo.
     * Mapeado pelo campo "motorista" em Viagem.
     */
    @OneToMany(mappedBy = "motorista", fetch = FetchType.LAZY, cascade = {})
    @JsonIgnore
    private List<Viagem> viagens = new ArrayList<>();

    /**
     * Um motorista pode ter várias jornadas registradas.
     * Mapeado pelo campo "motorista" em Jornada.
     */
    @OneToMany(mappedBy = "motorista", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Jornada> jornadas = new ArrayList<>();

    /**
     * Histórico de telemetria gerado enquanto o motorista estava no veículo.
     * Mapeado pelo campo "motorista" em Telemetria (via vínculo de viagem).
     * Lazy para não carregar tudo.
     */
    @OneToMany(mappedBy = "motorista", fetch = FetchType.LAZY, cascade = {})
    @JsonIgnore
    private List<Telemetria> historicoTelemetria = new ArrayList<>();

    // ── Auditoria ──────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Construtores ───────────────────────────────────────────

    public Motorista() {
    }

    // ── Getters e Setters ──────────────────────────────────────

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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCnh() {
        return cnh;
    }

    public void setCnh(String cnh) {
        this.cnh = cnh;
    }

    public String getCategoriaCnh() {
        return categoriaCnh;
    }

    public void setCategoriaCnh(String categoriaCnh) {
        this.categoriaCnh = categoriaCnh;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public List<Viagem> getViagens() {
        return viagens;
    }

    public void setViagens(List<Viagem> viagens) {
        this.viagens = viagens;
    }

    public List<Jornada> getJornadas() {
        return jornadas;
    }

    public void setJornadas(List<Jornada> jornadas) {
        this.jornadas = jornadas;
    }

    public List<Telemetria> getHistoricoTelemetria() {
        return historicoTelemetria;
    }

    public void setHistoricoTelemetria(List<Telemetria> historicoTelemetria) {
        this.historicoTelemetria = historicoTelemetria;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @Override
    public String toString() {
        return "Motorista{id=" + id + ", nome='" + nome + "', cpf='" + cpf + "', cnh='" + cnh + "'}";
    }
}
