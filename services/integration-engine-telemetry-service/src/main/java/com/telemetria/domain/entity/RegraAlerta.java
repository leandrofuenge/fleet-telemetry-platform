package com.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Regra configurável por tenant para o motor de alertas (RF12). */
@Entity
@Table(name = "regras_alerta", indexes = {
        @Index(name = "idx_regra_alerta_tenant_ativa", columnList = "tenant_id, ativo")
})
public class RegraAlerta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false, length = 120) private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 80) private TipoAlerta tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SeveridadeAlerta severidade;
    @Column(nullable = false, length = 80) private String campo;
    @Column(nullable = false, length = 10) private String operador;
    @Column(name = "valor_limite") private Double valorLimite;
    @Column(name = "cooldown_minutos", nullable = false) private Integer cooldownMinutos = 5;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "canais", columnDefinition = "json") private List<String> canais;
    @Column(nullable = false) private Boolean ativo = true;
    @CreationTimestamp @Column(name = "criado_em", updatable = false) private LocalDateTime criadoEm;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; } public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getNome() { return nome; } public void setNome(String nome) { this.nome = nome; }
    public TipoAlerta getTipo() { return tipo; } public void setTipo(TipoAlerta tipo) { this.tipo = tipo; }
    public SeveridadeAlerta getSeveridade() { return severidade; } public void setSeveridade(SeveridadeAlerta severidade) { this.severidade = severidade; }
    public String getCampo() { return campo; } public void setCampo(String campo) { this.campo = campo; }
    public String getOperador() { return operador; } public void setOperador(String operador) { this.operador = operador; }
    public Double getValorLimite() { return valorLimite; } public void setValorLimite(Double valorLimite) { this.valorLimite = valorLimite; }
    public Integer getCooldownMinutos() { return cooldownMinutos; } public void setCooldownMinutos(Integer cooldownMinutos) { this.cooldownMinutos = cooldownMinutos; }
    public List<String> getCanais() { return canais; } public void setCanais(List<String> canais) { this.canais = canais; }
    public Boolean getAtivo() { return ativo; } public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
