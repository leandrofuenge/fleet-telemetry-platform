package com.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alertas", indexes = {
        @Index(name = "idx_alerta_tenant", columnList = "tenant_id"),
        @Index(name = "idx_alerta_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_alerta_tipo", columnList = "tipo"),
        @Index(name = "idx_alerta_severidade", columnList = "severidade"),
        @Index(name = "idx_alerta_data", columnList = "data_hora"),
        @Index(name = "idx_alerta_lido", columnList = "lido"),
        @Index(name = "idx_alerta_resolvido", columnList = "resolvido")
})
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "veiculo_uuid", length = 36)
    private String veiculoUuid;

    @Column(name = "motorista_id")
    private Long motoristaId;

    @Column(name = "viagem_id")
    private Long viagemId;

    @Column(name = "telemetria_id")
    private Long telemetriaId;

    @Column(name = "regra_id")
    private Long regraId;

    @Column(name = "tipo", nullable = false, length = 80)
    @Enumerated(EnumType.STRING)
    private TipoAlerta tipo;

    @Column(name = "severidade", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SeveridadeAlerta severidade;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(name = "mensagem", nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "velocidade_kmh")
    private Double velocidadeKmh;

    @Column(name = "odometro_km")
    private Double odometroKm;

    @Column(name = "nome_local")
    private String nomeLocal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_contexto", columnDefinition = "json")
    private Map<String, Object> dadosContexto;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "lido", nullable = false)
    private Boolean lido;

    @Column(name = "lido_por")
    private String lidoPor;

    @Column(name = "data_hora_leitura")
    private LocalDateTime dataHoraLeitura;

    @Column(name = "resolvido", nullable = false)
    private Boolean resolvido;

    @Column(name = "resolvido_por")
    private String resolvidoPor;

    @Column(name = "data_hora_resolucao")
    private LocalDateTime dataHoraResolucao;

    @Column(name = "observacao_resolucao", columnDefinition = "TEXT")
    private String observacaoResolucao;

    @Column(name = "notificacao_enviada", nullable = false)
    private Boolean notificacaoEnviada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canais_notificados", columnDefinition = "json")
    private Object canaisNotificados;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "veiculo_id", insertable = false, updatable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "motorista_id", insertable = false, updatable = false)
    private Motorista motorista;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;

    // ===== CONSTRUTORES =====
    
    public Alerta() {
        this.uuid = UUID.randomUUID().toString();
        this.lido = false;
        this.resolvido = false;
        this.notificacaoEnviada = false;
    }

    /**
     * Construtor com Builder
     */
    private Alerta(Builder builder) {
        this.uuid = builder.uuid != null ? builder.uuid : UUID.randomUUID().toString();
        this.tenantId = builder.tenantId;
        this.veiculoId = builder.veiculoId;
        this.veiculoUuid = builder.veiculoUuid;
        this.motoristaId = builder.motoristaId;
        this.viagemId = builder.viagemId;
        this.telemetriaId = builder.telemetriaId;
        this.regraId = builder.regraId;
        this.tipo = builder.tipo;
        this.severidade = builder.severidade;
        this.categoria = builder.categoria;
        this.mensagem = builder.mensagem;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.velocidadeKmh = builder.velocidadeKmh;
        this.odometroKm = builder.odometroKm;
        this.nomeLocal = builder.nomeLocal;
        this.dadosContexto = builder.dadosContexto;
        this.dataHora = builder.dataHora;
        this.lido = builder.lido != null ? builder.lido : false;
        this.lidoPor = builder.lidoPor;
        this.dataHoraLeitura = builder.dataHoraLeitura;
        this.resolvido = builder.resolvido != null ? builder.resolvido : false;
        this.resolvidoPor = builder.resolvidoPor;
        this.dataHoraResolucao = builder.dataHoraResolucao;
        this.observacaoResolucao = builder.observacaoResolucao;
        this.notificacaoEnviada = builder.notificacaoEnviada != null ? builder.notificacaoEnviada : false;
        this.canaisNotificados = builder.canaisNotificados;
    }

    // ===== BUILDER =====
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uuid;
        private Long tenantId;
        private Long veiculoId;
        private String veiculoUuid;
        private Long motoristaId;
        private Long viagemId;
        private Long telemetriaId;
        private Long regraId;
        private TipoAlerta tipo;
        private SeveridadeAlerta severidade;
        private String categoria;
        private String mensagem;
        private Double latitude;
        private Double longitude;
        private Double velocidadeKmh;
        private Double odometroKm;
        private String nomeLocal;
        private Map<String, Object> dadosContexto;
        private LocalDateTime dataHora;
        private Boolean lido;
        private String lidoPor;
        private LocalDateTime dataHoraLeitura;
        private Boolean resolvido;
        private String resolvidoPor;
        private LocalDateTime dataHoraResolucao;
        private String observacaoResolucao;
        private Boolean notificacaoEnviada;
        private Object canaisNotificados;

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder veiculoId(Long veiculoId) {
            this.veiculoId = veiculoId;
            return this;
        }

        public Builder veiculoUuid(String veiculoUuid) {
            this.veiculoUuid = veiculoUuid;
            return this;
        }

        public Builder motoristaId(Long motoristaId) {
            this.motoristaId = motoristaId;
            return this;
        }

        public Builder viagemId(Long viagemId) {
            this.viagemId = viagemId;
            return this;
        }

        public Builder telemetriaId(Long telemetriaId) {
            this.telemetriaId = telemetriaId;
            return this;
        }

        public Builder regraId(Long regraId) {
            this.regraId = regraId;
            return this;
        }

        public Builder tipo(TipoAlerta tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder severidade(SeveridadeAlerta severidade) {
            this.severidade = severidade;
            return this;
        }

        public Builder categoria(String categoria) {
            this.categoria = categoria;
            return this;
        }

        public Builder mensagem(String mensagem) {
            this.mensagem = mensagem;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder velocidadeKmh(Double velocidadeKmh) {
            this.velocidadeKmh = velocidadeKmh;
            return this;
        }

        public Builder odometroKm(Double odometroKm) {
            this.odometroKm = odometroKm;
            return this;
        }

        public Builder nomeLocal(String nomeLocal) {
            this.nomeLocal = nomeLocal;
            return this;
        }

        public Builder dadosContexto(Map<String, Object> dadosContexto) {
            this.dadosContexto = dadosContexto;
            return this;
        }

        public Builder dataHora(LocalDateTime dataHora) {
            this.dataHora = dataHora;
            return this;
        }

        public Builder lido(Boolean lido) {
            this.lido = lido;
            return this;
        }

        public Builder lidoPor(String lidoPor) {
            this.lidoPor = lidoPor;
            return this;
        }

        public Builder dataHoraLeitura(LocalDateTime dataHoraLeitura) {
            this.dataHoraLeitura = dataHoraLeitura;
            return this;
        }

        public Builder resolvido(Boolean resolvido) {
            this.resolvido = resolvido;
            return this;
        }

        public Builder resolvidoPor(String resolvidoPor) {
            this.resolvidoPor = resolvidoPor;
            return this;
        }

        public Builder dataHoraResolucao(LocalDateTime dataHoraResolucao) {
            this.dataHoraResolucao = dataHoraResolucao;
            return this;
        }

        public Builder observacaoResolucao(String observacaoResolucao) {
            this.observacaoResolucao = observacaoResolucao;
            return this;
        }

        public Builder notificacaoEnviada(Boolean notificacaoEnviada) {
            this.notificacaoEnviada = notificacaoEnviada;
            return this;
        }

        public Builder canaisNotificados(Object canaisNotificados) {
            this.canaisNotificados = canaisNotificados;
            return this;
        }

        public Alerta build() {
            return new Alerta(this);
        }
    }

    // ===== GETTERS E SETTERS =====
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public Long getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Long veiculoId) { this.veiculoId = veiculoId; }
    
    public String getVeiculoUuid() { return veiculoUuid; }
    public void setVeiculoUuid(String veiculoUuid) { this.veiculoUuid = veiculoUuid; }
    
    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long motoristaId) { this.motoristaId = motoristaId; }
    
    public Long getViagemId() { return viagemId; }
    public void setViagemId(Long viagemId) { this.viagemId = viagemId; }
    
    public Long getTelemetriaId() { return telemetriaId; }
    public void setTelemetriaId(Long telemetriaId) { this.telemetriaId = telemetriaId; }
    
    public Long getRegraId() { return regraId; }
    public void setRegraId(Long regraId) { this.regraId = regraId; }
    
    public TipoAlerta getTipo() { return tipo; }
    public void setTipo(TipoAlerta tipo) { this.tipo = tipo; }
    
    public SeveridadeAlerta getSeveridade() { return severidade; }
    public void setSeveridade(SeveridadeAlerta severidade) { this.severidade = severidade; }
    
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public Double getVelocidadeKmh() { return velocidadeKmh; }
    public void setVelocidadeKmh(Double velocidadeKmh) { this.velocidadeKmh = velocidadeKmh; }
    
    public Double getOdometroKm() { return odometroKm; }
    public void setOdometroKm(Double odometroKm) { this.odometroKm = odometroKm; }
    
    public String getNomeLocal() { return nomeLocal; }
    public void setNomeLocal(String nomeLocal) { this.nomeLocal = nomeLocal; }
    
    public Map<String, Object> getDadosContexto() { return dadosContexto; }
    public void setDadosContexto(Map<String, Object> dadosContexto) { this.dadosContexto = dadosContexto; }
    
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    
    public Boolean getLido() { return lido; }
    public void setLido(Boolean lido) { this.lido = lido; }
    
    public String getLidoPor() { return lidoPor; }
    public void setLidoPor(String lidoPor) { this.lidoPor = lidoPor; }
    
    public LocalDateTime getDataHoraLeitura() { return dataHoraLeitura; }
    public void setDataHoraLeitura(LocalDateTime dataHoraLeitura) { this.dataHoraLeitura = dataHoraLeitura; }
    
    public Boolean getResolvido() { return resolvido; }
    public void setResolvido(Boolean resolvido) { this.resolvido = resolvido; }
    
    public String getResolvidoPor() { return resolvidoPor; }
    public void setResolvidoPor(String resolvidoPor) { this.resolvidoPor = resolvidoPor; }
    
    public LocalDateTime getDataHoraResolucao() { return dataHoraResolucao; }
    public void setDataHoraResolucao(LocalDateTime dataHoraResolucao) { this.dataHoraResolucao = dataHoraResolucao; }
    
    public String getObservacaoResolucao() { return observacaoResolucao; }
    public void setObservacaoResolucao(String observacaoResolucao) { this.observacaoResolucao = observacaoResolucao; }
    
    public Boolean getNotificacaoEnviada() { return notificacaoEnviada; }
    public void setNotificacaoEnviada(Boolean notificacaoEnviada) { this.notificacaoEnviada = notificacaoEnviada; }
    
    public Object getCanaisNotificados() { return canaisNotificados; }
    public void setCanaisNotificados(Object canaisNotificados) { this.canaisNotificados = canaisNotificados; }
    
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    
    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }
    
    public Viagem getViagem() { return viagem; }
    public void setViagem(Viagem viagem) { this.viagem = viagem; }
}