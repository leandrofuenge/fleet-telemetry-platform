package com.app.telemetria.domain.entity;

import java.time.LocalDate;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "motoristas", 
       indexes = {
           @Index(name = "idx_mot_cpf", columnList = "cpf"),
           @Index(name = "idx_mot_cnh", columnList = "cnh"),
           @Index(name = "idx_mot_tenant", columnList = "tenant_id"),
           @Index(name = "idx_mot_ativo", columnList = "ativo")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_motorista_cpf_tenant", 
                             columnNames = {"cpf", "tenant_id"})
       })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Motorista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== NOVO: Tenant ID =====
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    // RN-MOT-001: CPF com validação
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$", 
             message = "CPF inválido. Use formato 123.456.789-00 ou 12345678900")
    @Column(nullable = false, length = 14)
    private String cpf;

    // RN-MOT-002: CNH
    @NotBlank(message = "CNH é obrigatória")
    @Column(nullable = false, unique = true, length = 20)
    private String cnh;

    @NotBlank(message = "Categoria da CNH é obrigatória")
    @Column(name = "categoria_cnh", nullable = false, length = 5)
    private String categoriaCnh;

    // RN-MOT-002: Data de vencimento da CNH
    @Column(name = "data_venc_cnh")
    private LocalDate dataVencimentoCnh;

    // RN-MOT-003: ASO
    @Column(name = "data_venc_aso")
    private LocalDate dataVencimentoAso;

    // RN-MOT-003: MOPP para cargas perigosas
    @Column(name = "mopp_valido")
    private Boolean moppValido = false;

    // RN-MOT-004: Score de comportamento (0-1000)
    @Column(name = "score", nullable = false)
    private Integer score = 1000;

    @Column(unique = true, length = 200)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    // ── Relacionamentos ────────────────────────────────────────
    @OneToMany(mappedBy = "motorista", fetch = FetchType.LAZY, cascade = {})
    @JsonIgnore
    private List<Viagem> viagens = new ArrayList<>();

    @OneToMany(mappedBy = "motorista", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Jornada> jornadas = new ArrayList<>();

    @OneToMany(mappedBy = "motorista", fetch = FetchType.LAZY, cascade = {})
    @JsonIgnore
    private List<Telemetria> historicoTelemetria = new ArrayList<>();

    // ── Histórico de score ──────────────────────────────────────
    @OneToMany(mappedBy = "motorista", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<HistoricoScoreMotorista> historicoScore = new ArrayList<>();

    // ── Auditoria ──────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // Construtores
    public Motorista() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getCnh() { return cnh; }
    public void setCnh(String cnh) { this.cnh = cnh; }

    public String getCategoriaCnh() { return categoriaCnh; }
    public void setCategoriaCnh(String categoriaCnh) { this.categoriaCnh = categoriaCnh; }

    public LocalDate getDataVencimentoCnh() { return dataVencimentoCnh; }
    public void setDataVencimentoCnh(LocalDate dataVencimentoCnh) { this.dataVencimentoCnh = dataVencimentoCnh; }

    public LocalDate getDataVencimentoAso() { return dataVencimentoAso; }
    public void setDataVencimentoAso(LocalDate dataVencimentoAso) { this.dataVencimentoAso = dataVencimentoAso; }

    public Boolean getMoppValido() { return moppValido; }
    public void setMoppValido(Boolean moppValido) { this.moppValido = moppValido; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public List<Viagem> getViagens() { return viagens; }
    public void setViagens(List<Viagem> viagens) { this.viagens = viagens; }

    public List<Jornada> getJornadas() { return jornadas; }
    public void setJornadas(List<Jornada> jornadas) { this.jornadas = jornadas; }

    public List<Telemetria> getHistoricoTelemetria() { return historicoTelemetria; }
    public void setHistoricoTelemetria(List<Telemetria> historicoTelemetria) { this.historicoTelemetria = historicoTelemetria; }

    public List<HistoricoScoreMotorista> getHistoricoScore() { return historicoScore; }
    public void setHistoricoScore(List<HistoricoScoreMotorista> historicoScore) { this.historicoScore = historicoScore; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    @Override
    public String toString() {
        return "Motorista{id=" + id + ", nome='" + nome + "', cpf='" + cpf + "'}";
    }
}