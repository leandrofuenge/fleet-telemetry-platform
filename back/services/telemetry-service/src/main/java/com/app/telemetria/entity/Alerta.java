package com.app.telemetria.entity;

import com.app.telemetria.enums.SeveridadeAlerta;
import com.app.telemetria.enums.TipoAlerta;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    @Column(name = "severidade", nullable = false, length = 10)
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

    // ── Relacionamentos JPA (objetos completos) ────────────────

    /**
     * Veículo que gerou o alerta.
     * Bidirecional com Veiculo — inserção controlada pelas colunas FK acima.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", insertable = false, updatable = false)
    private Veiculo veiculo;

    /**
     * Motorista envolvido no alerta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", insertable = false, updatable = false)
    private Motorista motorista;

    /**
     * Viagem durante a qual o alerta foi gerado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;

    // ── Construtores ───────────────────────────────────────────

    public Alerta() {
        this.uuid = UUID.randomUUID().toString();
        this.lido = false;
        this.resolvido = false;
        this.notificacaoEnviada = false;
    }

    // ── Getters e Setters ──────────────────────────────────────

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

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public String getVeiculoUuid() {
        return veiculoUuid;
    }

    public void setVeiculoUuid(String veiculoUuid) {
        this.veiculoUuid = veiculoUuid;
    }

    public Long getMotoristaId() {
        return motoristaId;
    }

    public void setMotoristaId(Long motoristaId) {
        this.motoristaId = motoristaId;
    }

    public Long getViagemId() {
        return viagemId;
    }

    public void setViagemId(Long viagemId) {
        this.viagemId = viagemId;
    }

    public Long getTelemetriaId() {
        return telemetriaId;
    }

    public void setTelemetriaId(Long telemetriaId) {
        this.telemetriaId = telemetriaId;
    }

    public Long getRegraId() {
        return regraId;
    }

    public void setRegraId(Long regraId) {
        this.regraId = regraId;
    }

    public TipoAlerta getTipo() {
        return tipo;
    }

    public void setTipo(TipoAlerta tipo) {
        this.tipo = tipo;
    }

    public SeveridadeAlerta getSeveridade() {
        return severidade;
    }

    public void setSeveridade(SeveridadeAlerta severidade) {
        this.severidade = severidade;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getVelocidadeKmh() {
        return velocidadeKmh;
    }

    public void setVelocidadeKmh(Double velocidadeKmh) {
        this.velocidadeKmh = velocidadeKmh;
    }

    public Double getOdometroKm() {
        return odometroKm;
    }

    public void setOdometroKm(Double odometroKm) {
        this.odometroKm = odometroKm;
    }

    public String getNomeLocal() {
        return nomeLocal;
    }

    public void setNomeLocal(String nomeLocal) {
        this.nomeLocal = nomeLocal;
    }

    public Map<String, Object> getDadosContexto() {
        return dadosContexto;
    }

    public void setDadosContexto(Map<String, Object> dadosContexto) {
        this.dadosContexto = dadosContexto;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Boolean getLido() {
        return lido;
    }

    public void setLido(Boolean lido) {
        this.lido = lido;
    }

    public String getLidoPor() {
        return lidoPor;
    }

    public void setLidoPor(String lidoPor) {
        this.lidoPor = lidoPor;
    }

    public LocalDateTime getDataHoraLeitura() {
        return dataHoraLeitura;
    }

    public void setDataHoraLeitura(LocalDateTime dataHoraLeitura) {
        this.dataHoraLeitura = dataHoraLeitura;
    }

    public Boolean getResolvido() {
        return resolvido;
    }

    public void setResolvido(Boolean resolvido) {
        this.resolvido = resolvido;
    }

    public String getResolvidoPor() {
        return resolvidoPor;
    }

    public void setResolvidoPor(String resolvidoPor) {
        this.resolvidoPor = resolvidoPor;
    }

    public LocalDateTime getDataHoraResolucao() {
        return dataHoraResolucao;
    }

    public void setDataHoraResolucao(LocalDateTime dataHoraResolucao) {
        this.dataHoraResolucao = dataHoraResolucao;
    }

    public String getObservacaoResolucao() {
        return observacaoResolucao;
    }

    public void setObservacaoResolucao(String observacaoResolucao) {
        this.observacaoResolucao = observacaoResolucao;
    }

    public Boolean getNotificacaoEnviada() {
        return notificacaoEnviada;
    }

    public void setNotificacaoEnviada(Boolean notificacaoEnviada) {
        this.notificacaoEnviada = notificacaoEnviada;
    }

    public Object getCanaisNotificados() {
        return canaisNotificados;
    }

    public void setCanaisNotificados(Object canaisNotificados) {
        this.canaisNotificados = canaisNotificados;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public Motorista getMotorista() {
        return motorista;
    }

    public void setMotorista(Motorista motorista) {
        this.motorista = motorista;
    }

    public Viagem getViagem() {
        return viagem;
    }

    public void setViagem(Viagem viagem) {
        this.viagem = viagem;
    }
}
