package com.app.telemetria.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.app.telemetria.domain.enums.Perfil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "usuarios", indexes = {
        @Index(name = "idx_usr_login", columnList = "login"),
        @Index(name = "idx_usr_cpf", columnList = "cpf"),
        @Index(name = "idx_usr_email", columnList = "email")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Login é obrigatório")
    @Column(nullable = false, unique = true, length = 100)
    private String login;

    @NotBlank(message = "Senha é obrigatória")
    @Column(nullable = false)
    private String senha;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @Column(unique = true, length = 200)
    private String email;

    @Column(unique = true, nullable = false, length = 14)
    private String cpf;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    // ===== RN-USR-001: Política de Senha =====
    
    @Column(name = "data_expiracao_senha")
    private LocalDate dataExpiracaoSenha;
    
    @Column(name = "tentativas_falha")
    private Integer tentativasFalha = 0;
    
    @Column(name = "ultima_tentativa_falha")
    private LocalDateTime ultimaTentativaFalha;
    
    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;
    
    // ===== RN-USR-002: MFA =====
    
    @Column(name = "mfa_secret", length = 32)
    private String mfaSecret;
    
    @Column(name = "mfa_ativado")
    private Boolean mfaAtivado = false;

    // ── Relacionamentos ────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", unique = true)
    private Motorista motorista;
    
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<HistoricoSenha> historicoSenhas = new ArrayList<>();
    
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SessaoAtiva> sessoesAtivas = new ArrayList<>();

    // ── Auditoria ──────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Construtores ───────────────────────────────────────────
    public Usuario() {
        this.ativo = true;
        this.tentativasFalha = 0;
        this.mfaAtivado = false;
    }

    // ===== Métodos de utilidade =====
    
    public void registrarTentativaFalha() {
        this.tentativasFalha = (this.tentativasFalha == null ? 0 : this.tentativasFalha) + 1;
        this.ultimaTentativaFalha = LocalDateTime.now();
        
        // RN-USR-001: Bloqueio após 5 falhas em 10 minutos
        if (this.tentativasFalha >= 5) {
            this.bloqueadoAte = LocalDateTime.now().plusMinutes(10);
        }
    }
    
    public void resetarTentativasFalha() {
        this.tentativasFalha = 0;
        this.ultimaTentativaFalha = null;
        this.bloqueadoAte = null;
    }
    
    public boolean isBloqueado() {
        if (this.bloqueadoAte == null) return false;
        return LocalDateTime.now().isBefore(this.bloqueadoAte);
    }
    
    public boolean isSenhaExpirada() {
        if (this.dataExpiracaoSenha == null) return false;
        return LocalDate.now().isAfter(this.dataExpiracaoSenha);
    }
    
    public void atualizarExpiracaoSenha() {
        // RN-USR-001: Expirar após 90 dias
        this.dataExpiracaoSenha = LocalDate.now().plusDays(90);
    }
    
    // ===== Getters e Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }

    public LocalDateTime getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(LocalDateTime ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }

    public LocalDate getDataExpiracaoSenha() { return dataExpiracaoSenha; }
    public void setDataExpiracaoSenha(LocalDate dataExpiracaoSenha) { this.dataExpiracaoSenha = dataExpiracaoSenha; }

    public Integer getTentativasFalha() { return tentativasFalha; }
    public void setTentativasFalha(Integer tentativasFalha) { this.tentativasFalha = tentativasFalha; }

    public LocalDateTime getUltimaTentativaFalha() { return ultimaTentativaFalha; }
    public void setUltimaTentativaFalha(LocalDateTime ultimaTentativaFalha) { this.ultimaTentativaFalha = ultimaTentativaFalha; }

    public LocalDateTime getBloqueadoAte() { return bloqueadoAte; }
    public void setBloqueadoAte(LocalDateTime bloqueadoAte) { this.bloqueadoAte = bloqueadoAte; }

    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }

    public Boolean getMfaAtivado() { return mfaAtivado; }
    public void setMfaAtivado(Boolean mfaAtivado) { this.mfaAtivado = mfaAtivado; }

    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }

    public List<HistoricoSenha> getHistoricoSenhas() { return historicoSenhas; }
    public void setHistoricoSenhas(List<HistoricoSenha> historicoSenhas) { this.historicoSenhas = historicoSenhas; }

    public List<SessaoAtiva> getSessoesAtivas() { return sessoesAtivas; }
    public void setSessoesAtivas(List<SessaoAtiva> sessoesAtivas) { this.sessoesAtivas = sessoesAtivas; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}