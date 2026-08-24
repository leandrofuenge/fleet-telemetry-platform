package com.telemetria.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telemetria.domain.enums.PlanoTenant;
import com.telemetria.domain.enums.StatusTenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenant_cnpj", columnList = "cnpj", unique = true),
        @Index(name = "idx_tenant_status", columnList = "status")
})
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_razao_social", nullable = false, length = 255)
    private String nomeRazaoSocial;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanoTenant plano = PlanoTenant.STARTER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTenant status = StatusTenant.TRIAL;

    @Column(name = "trial_inicio", nullable = false)
    private LocalDate trialInicio;

    @Column(name = "trial_expira_em", nullable = false)
    private LocalDate trialExpiraEm;

    @Column(name = "dados_preservados_ate")
    private LocalDate dadosPreservadosAte;

    @Column(length = 200)
    private String email;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public Tenant() {
        LocalDate hoje = LocalDate.now();
        this.trialInicio = hoje;
        this.trialExpiraEm = hoje.plusDays(14);
    }

    public boolean estaAtivoParaOperacao() {
        return status == StatusTenant.ATIVO || (status == StatusTenant.TRIAL && !trialExpiraEm.isBefore(LocalDate.now()));
    }

    public void expirarTrialSeNecessario() {
        if (status == StatusTenant.TRIAL && trialExpiraEm.isBefore(LocalDate.now())) {
            status = StatusTenant.INATIVO;
            dadosPreservadosAte = LocalDate.now().plusDays(30);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeRazaoSocial() { return nomeRazaoSocial; }
    public void setNomeRazaoSocial(String nomeRazaoSocial) { this.nomeRazaoSocial = nomeRazaoSocial; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public PlanoTenant getPlano() { return plano; }
    public void setPlano(PlanoTenant plano) { this.plano = plano; }
    public StatusTenant getStatus() { return status; }
    public void setStatus(StatusTenant status) { this.status = status; }
    public LocalDate getTrialInicio() { return trialInicio; }
    public void setTrialInicio(LocalDate trialInicio) { this.trialInicio = trialInicio; }
    public LocalDate getTrialExpiraEm() { return trialExpiraEm; }
    public void setTrialExpiraEm(LocalDate trialExpiraEm) { this.trialExpiraEm = trialExpiraEm; }
    public LocalDate getDadosPreservadosAte() { return dadosPreservadosAte; }
    public void setDadosPreservadosAte(LocalDate dadosPreservadosAte) { this.dadosPreservadosAte = dadosPreservadosAte; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
