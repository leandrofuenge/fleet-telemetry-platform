package com.app.routing.entity;

import com.app.routing.enums.PerfilOsrm;
import com.app.routing.enums.StatusRota;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rotas", indexes = {
        @Index(name = "idx_rota_tenant", columnList = "tenant_id"),
        @Index(name = "idx_rota_status", columnList = "status"),
        @Index(name = "idx_rota_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_rota_motorista", columnList = "motorista_id"),
        @Index(name = "idx_rota_ativa", columnList = "ativa"),
        @Index(name = "idx_rota_datas", columnList = "data_inicio_plan, data_fim_plan"),
        @Index(name = "idx_rota_uuid", columnList = "uuid")
})
public class Rota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "nome", nullable = false)
    private String nome;

    // ── Origem ────────────────────────────────────────────────
    @Column(name = "origem_nome", nullable = false)
    private String origemNome;

    @Column(name = "origem_latitude", nullable = false)
    private Double origemLatitude;

    @Column(name = "origem_longitude", nullable = false)
    private Double origemLongitude;

    @Column(name = "origem_endereco", length = 500)
    private String origemEndereco;

    @Column(name = "origem_cep", length = 10)
    private String origemCep;

    // ── Destino ───────────────────────────────────────────────
    @Column(name = "destino_nome", nullable = false)
    private String destinoNome;

    @Column(name = "destino_latitude", nullable = false)
    private Double destinoLatitude;

    @Column(name = "destino_longitude", nullable = false)
    private Double destinoLongitude;

    @Column(name = "destino_endereco", length = 500)
    private String destinoEndereco;

    @Column(name = "destino_cep", length = 10)
    private String destinoCep;

    // ── Dados OSRM ────────────────────────────────────────────
    @Column(name = "distancia_km")
    private Double distanciaKm;

    @Column(name = "tempo_estimado_min")
    private Integer tempoEstimadoMin;

    @Column(name = "polyline_encoded", columnDefinition = "TEXT")
    private String polylineEncoded;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rota_geojson", columnDefinition = "json")
    private Object rotaGeojson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pontos_rota", columnDefinition = "json")
    private List<CoordenadasDto> pontosRota;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pontos_snap", columnDefinition = "json")
    private List<CoordenadasDto> pontosSnap;

    @Column(name = "osrm_request_id", length = 36)
    private String osrmRequestId;

    // ── Paradas ───────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "paradas", columnDefinition = "json")
    private List<ParadaDto> paradas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sequencia_otimizada", columnDefinition = "json")
    private List<ParadaDto> sequenciaOtimizada;

    @Column(name = "total_paradas", nullable = false)
    private Integer totalParadas;

    // ── Custos estimados ──────────────────────────────────────
    @Column(name = "custo_pedagio_est", precision = 10, scale = 2)
    private BigDecimal custoPedagioEst;

    @Column(name = "custo_combustivel_est", precision = 10, scale = 2)
    private BigDecimal custoCombustivelEst;

    @Column(name = "custo_total_est", precision = 10, scale = 2)
    private BigDecimal custoTotalEst;

    // ── Desvio ────────────────────────────────────────────────
    @Column(name = "tolerancia_desvio_m", nullable = false)
    private Double toleranciaDesvioM;

    @Column(name = "threshold_alerta_m", nullable = false)
    private Double thresholdAlertaM;

    @Column(name = "max_km_extras_alerta", nullable = false)
    private Double maxKmExtrasAlerta;

    // ── Vínculos ──────────────────────────────────────────────
    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "veiculo_uuid", length = 36)
    private String veiculoUuid;

    @Column(name = "motorista_id")
    private Long motoristaId;

    @Column(name = "motorista_uuid", length = 36)
    private String motoristaUuid;

    @Column(name = "carga_uuid", length = 36)
    private String cargaUuid;

    // ── Datas ─────────────────────────────────────────────────
    @Column(name = "data_inicio_plan")
    private LocalDateTime dataInicioPlan;

    @Column(name = "data_fim_plan")
    private LocalDateTime dataFimPlan;

    @Column(name = "data_inicio_real")
    private LocalDateTime dataInicioReal;

    @Column(name = "data_fim_real")
    private LocalDateTime dataFimReal;

    // ── Documentação Fiscal ───────────────────────────────────
    @Column(name = "mdfe_chave", length = 50)
    private String mdfeChave;

    @Column(name = "cte_chave", length = 50)
    private String cteChave;

    @Column(name = "ciot_codigo", length = 30)
    private String ciotCodigo;

    // ── Perfil OSRM ───────────────────────────────────────────
    @Column(name = "perfil_osrm", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private PerfilOsrm perfilOsrm;

    // ── Status ────────────────────────────────────────────────
    @Column(name = "status", nullable = false, length = 25)
    @Enumerated(EnumType.STRING)
    private StatusRota status;

    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;

    @Column(name = "ativa", nullable = false)
    private Boolean ativa;

    @Column(name = "criado_por")
    private String criadoPor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Relacionamentos ───────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", insertable = false, updatable = false)
    private VeiculoCache veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", insertable = false, updatable = false)
    private MotoristaCache motorista;

    @OneToMany(mappedBy = "rota", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Viagem> viagens;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão (sem argumentos).
     * Inicializa os campos com os valores padrão definidos nos @Builder.Default
     * originais.
     */
    public Rota() {
        this.uuid = UUID.randomUUID().toString();
        this.totalParadas = 0;
        this.toleranciaDesvioM = 100.0;
        this.thresholdAlertaM = 50.0;
        this.maxKmExtrasAlerta = 2.0;
        this.perfilOsrm = PerfilOsrm.CAMINHAO;
        this.status = StatusRota.PLANEJADA;
        this.ativa = true;
    }

    /**
     * Construtor privado com todos os campos.
     * Usado internamente pelo Builder.
     */
    private Rota(Long id, String uuid, Long tenantId, String nome,
            String origemNome, Double origemLatitude, Double origemLongitude,
            String origemEndereco, String origemCep,
            String destinoNome, Double destinoLatitude, Double destinoLongitude,
            String destinoEndereco, String destinoCep,
            Double distanciaKm, Integer tempoEstimadoMin, String polylineEncoded,
            Object rotaGeojson, List<CoordenadasDto> pontosRota, List<CoordenadasDto> pontosSnap,
            String osrmRequestId, List<ParadaDto> paradas, List<ParadaDto> sequenciaOtimizada,
            Integer totalParadas, BigDecimal custoPedagioEst, BigDecimal custoCombustivelEst,
            BigDecimal custoTotalEst, Double toleranciaDesvioM, Double thresholdAlertaM,
            Double maxKmExtrasAlerta, Long veiculoId, String veiculoUuid, Long motoristaId,
            String motoristaUuid, String cargaUuid, LocalDateTime dataInicioPlan,
            LocalDateTime dataFimPlan, LocalDateTime dataInicioReal, LocalDateTime dataFimReal,
            String mdfeChave, String cteChave, String ciotCodigo, PerfilOsrm perfilOsrm,
            StatusRota status, String motivoCancelamento, Boolean ativa, String criadoPor,
            LocalDateTime criadoEm, LocalDateTime atualizadoEm,
            VeiculoCache veiculo, MotoristaCache motorista, List<Viagem> viagens) {
        this.id = id;
        this.uuid = uuid != null ? uuid : UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.nome = nome;
        this.origemNome = origemNome;
        this.origemLatitude = origemLatitude;
        this.origemLongitude = origemLongitude;
        this.origemEndereco = origemEndereco;
        this.origemCep = origemCep;
        this.destinoNome = destinoNome;
        this.destinoLatitude = destinoLatitude;
        this.destinoLongitude = destinoLongitude;
        this.destinoEndereco = destinoEndereco;
        this.destinoCep = destinoCep;
        this.distanciaKm = distanciaKm;
        this.tempoEstimadoMin = tempoEstimadoMin;
        this.polylineEncoded = polylineEncoded;
        this.rotaGeojson = rotaGeojson;
        this.pontosRota = pontosRota;
        this.pontosSnap = pontosSnap;
        this.osrmRequestId = osrmRequestId;
        this.paradas = paradas;
        this.sequenciaOtimizada = sequenciaOtimizada;
        this.totalParadas = totalParadas != null ? totalParadas : 0;
        this.custoPedagioEst = custoPedagioEst;
        this.custoCombustivelEst = custoCombustivelEst;
        this.custoTotalEst = custoTotalEst;
        this.toleranciaDesvioM = toleranciaDesvioM != null ? toleranciaDesvioM : 100.0;
        this.thresholdAlertaM = thresholdAlertaM != null ? thresholdAlertaM : 50.0;
        this.maxKmExtrasAlerta = maxKmExtrasAlerta != null ? maxKmExtrasAlerta : 2.0;
        this.veiculoId = veiculoId;
        this.veiculoUuid = veiculoUuid;
        this.motoristaId = motoristaId;
        this.motoristaUuid = motoristaUuid;
        this.cargaUuid = cargaUuid;
        this.dataInicioPlan = dataInicioPlan;
        this.dataFimPlan = dataFimPlan;
        this.dataInicioReal = dataInicioReal;
        this.dataFimReal = dataFimReal;
        this.mdfeChave = mdfeChave;
        this.cteChave = cteChave;
        this.ciotCodigo = ciotCodigo;
        this.perfilOsrm = perfilOsrm != null ? perfilOsrm : PerfilOsrm.CAMINHAO;
        this.status = status != null ? status : StatusRota.PLANEJADA;
        this.motivoCancelamento = motivoCancelamento;
        this.ativa = ativa != null ? ativa : true;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.veiculo = veiculo;
        this.motorista = motorista;
        this.viagens = viagens;
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

    public String getOrigemNome() {
        return origemNome;
    }

    public void setOrigemNome(String origemNome) {
        this.origemNome = origemNome;
    }

    public Double getOrigemLatitude() {
        return origemLatitude;
    }

    public void setOrigemLatitude(Double origemLatitude) {
        this.origemLatitude = origemLatitude;
    }

    public Double getOrigemLongitude() {
        return origemLongitude;
    }

    public void setOrigemLongitude(Double origemLongitude) {
        this.origemLongitude = origemLongitude;
    }

    public String getOrigemEndereco() {
        return origemEndereco;
    }

    public void setOrigemEndereco(String origemEndereco) {
        this.origemEndereco = origemEndereco;
    }

    public String getOrigemCep() {
        return origemCep;
    }

    public void setOrigemCep(String origemCep) {
        this.origemCep = origemCep;
    }

    public String getDestinoNome() {
        return destinoNome;
    }

    public void setDestinoNome(String destinoNome) {
        this.destinoNome = destinoNome;
    }

    public Double getDestinoLatitude() {
        return destinoLatitude;
    }

    public void setDestinoLatitude(Double destinoLatitude) {
        this.destinoLatitude = destinoLatitude;
    }

    public Double getDestinoLongitude() {
        return destinoLongitude;
    }

    public void setDestinoLongitude(Double destinoLongitude) {
        this.destinoLongitude = destinoLongitude;
    }

    public String getDestinoEndereco() {
        return destinoEndereco;
    }

    public void setDestinoEndereco(String destinoEndereco) {
        this.destinoEndereco = destinoEndereco;
    }

    public String getDestinoCep() {
        return destinoCep;
    }

    public void setDestinoCep(String destinoCep) {
        this.destinoCep = destinoCep;
    }

    public Double getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(Double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public Integer getTempoEstimadoMin() {
        return tempoEstimadoMin;
    }

    public void setTempoEstimadoMin(Integer tempoEstimadoMin) {
        this.tempoEstimadoMin = tempoEstimadoMin;
    }

    public String getPolylineEncoded() {
        return polylineEncoded;
    }

    public void setPolylineEncoded(String polylineEncoded) {
        this.polylineEncoded = polylineEncoded;
    }

    public Object getRotaGeojson() {
        return rotaGeojson;
    }

    public void setRotaGeojson(Object rotaGeojson) {
        this.rotaGeojson = rotaGeojson;
    }

    public List<CoordenadasDto> getPontosRota() {
        return pontosRota;
    }

    public void setPontosRota(List<CoordenadasDto> pontosRota) {
        this.pontosRota = pontosRota;
    }

    public List<CoordenadasDto> getPontosSnap() {
        return pontosSnap;
    }

    public void setPontosSnap(List<CoordenadasDto> pontosSnap) {
        this.pontosSnap = pontosSnap;
    }

    public String getOsrmRequestId() {
        return osrmRequestId;
    }

    public void setOsrmRequestId(String osrmRequestId) {
        this.osrmRequestId = osrmRequestId;
    }

    public List<ParadaDto> getParadas() {
        return paradas;
    }

    public void setParadas(List<ParadaDto> paradas) {
        this.paradas = paradas;
    }

    public List<ParadaDto> getSequenciaOtimizada() {
        return sequenciaOtimizada;
    }

    public void setSequenciaOtimizada(List<ParadaDto> sequenciaOtimizada) {
        this.sequenciaOtimizada = sequenciaOtimizada;
    }

    public Integer getTotalParadas() {
        return totalParadas;
    }

    public void setTotalParadas(Integer totalParadas) {
        this.totalParadas = totalParadas;
    }

    public BigDecimal getCustoPedagioEst() {
        return custoPedagioEst;
    }

    public void setCustoPedagioEst(BigDecimal custoPedagioEst) {
        this.custoPedagioEst = custoPedagioEst;
    }

    public BigDecimal getCustoCombustivelEst() {
        return custoCombustivelEst;
    }

    public void setCustoCombustivelEst(BigDecimal custoCombustivelEst) {
        this.custoCombustivelEst = custoCombustivelEst;
    }

    public BigDecimal getCustoTotalEst() {
        return custoTotalEst;
    }

    public void setCustoTotalEst(BigDecimal custoTotalEst) {
        this.custoTotalEst = custoTotalEst;
    }

    public Double getToleranciaDesvioM() {
        return toleranciaDesvioM;
    }

    public void setToleranciaDesvioM(Double toleranciaDesvioM) {
        this.toleranciaDesvioM = toleranciaDesvioM;
    }

    public Double getThresholdAlertaM() {
        return thresholdAlertaM;
    }

    public void setThresholdAlertaM(Double thresholdAlertaM) {
        this.thresholdAlertaM = thresholdAlertaM;
    }

    public Double getMaxKmExtrasAlerta() {
        return maxKmExtrasAlerta;
    }

    public void setMaxKmExtrasAlerta(Double maxKmExtrasAlerta) {
        this.maxKmExtrasAlerta = maxKmExtrasAlerta;
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

    public String getMotoristaUuid() {
        return motoristaUuid;
    }

    public void setMotoristaUuid(String motoristaUuid) {
        this.motoristaUuid = motoristaUuid;
    }

    public String getCargaUuid() {
        return cargaUuid;
    }

    public void setCargaUuid(String cargaUuid) {
        this.cargaUuid = cargaUuid;
    }

    public LocalDateTime getDataInicioPlan() {
        return dataInicioPlan;
    }

    public void setDataInicioPlan(LocalDateTime dataInicioPlan) {
        this.dataInicioPlan = dataInicioPlan;
    }

    public LocalDateTime getDataFimPlan() {
        return dataFimPlan;
    }

    public void setDataFimPlan(LocalDateTime dataFimPlan) {
        this.dataFimPlan = dataFimPlan;
    }

    public LocalDateTime getDataInicioReal() {
        return dataInicioReal;
    }

    public void setDataInicioReal(LocalDateTime dataInicioReal) {
        this.dataInicioReal = dataInicioReal;
    }

    public LocalDateTime getDataFimReal() {
        return dataFimReal;
    }

    public void setDataFimReal(LocalDateTime dataFimReal) {
        this.dataFimReal = dataFimReal;
    }

    public String getMdfeChave() {
        return mdfeChave;
    }

    public void setMdfeChave(String mdfeChave) {
        this.mdfeChave = mdfeChave;
    }

    public String getCteChave() {
        return cteChave;
    }

    public void setCteChave(String cteChave) {
        this.cteChave = cteChave;
    }

    public String getCiotCodigo() {
        return ciotCodigo;
    }

    public void setCiotCodigo(String ciotCodigo) {
        this.ciotCodigo = ciotCodigo;
    }

    public PerfilOsrm getPerfilOsrm() {
        return perfilOsrm;
    }

    public void setPerfilOsrm(PerfilOsrm perfilOsrm) {
        this.perfilOsrm = perfilOsrm;
    }

    public StatusRota getStatus() {
        return status;
    }

    public void setStatus(StatusRota status) {
        this.status = status;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public Boolean getAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(String criadoPor) {
        this.criadoPor = criadoPor;
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

    public VeiculoCache getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(VeiculoCache veiculo) {
        this.veiculo = veiculo;
    }

    public MotoristaCache getMotorista() {
        return motorista;
    }

    public void setMotorista(MotoristaCache motorista) {
        this.motorista = motorista;
    }

    public List<Viagem> getViagens() {
        return viagens;
    }

    public void setViagens(List<Viagem> viagens) {
        this.viagens = viagens;
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String uuid = UUID.randomUUID().toString();
        private Long tenantId;
        private String nome;
        private String origemNome;
        private Double origemLatitude;
        private Double origemLongitude;
        private String origemEndereco;
        private String origemCep;
        private String destinoNome;
        private Double destinoLatitude;
        private Double destinoLongitude;
        private String destinoEndereco;
        private String destinoCep;
        private Double distanciaKm;
        private Integer tempoEstimadoMin;
        private String polylineEncoded;
        private Object rotaGeojson;
        private List<CoordenadasDto> pontosRota;
        private List<CoordenadasDto> pontosSnap;
        private String osrmRequestId;
        private List<ParadaDto> paradas;
        private List<ParadaDto> sequenciaOtimizada;
        private Integer totalParadas = 0;
        private BigDecimal custoPedagioEst;
        private BigDecimal custoCombustivelEst;
        private BigDecimal custoTotalEst;
        private Double toleranciaDesvioM = 100.0;
        private Double thresholdAlertaM = 50.0;
        private Double maxKmExtrasAlerta = 2.0;
        private Long veiculoId;
        private String veiculoUuid;
        private Long motoristaId;
        private String motoristaUuid;
        private String cargaUuid;
        private LocalDateTime dataInicioPlan;
        private LocalDateTime dataFimPlan;
        private LocalDateTime dataInicioReal;
        private LocalDateTime dataFimReal;
        private String mdfeChave;
        private String cteChave;
        private String ciotCodigo;
        private PerfilOsrm perfilOsrm = PerfilOsrm.CAMINHAO;
        private StatusRota status = StatusRota.PLANEJADA;
        private String motivoCancelamento;
        private Boolean ativa = true;
        private String criadoPor;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;
        private VeiculoCache veiculo;
        private MotoristaCache motorista;
        private List<Viagem> viagens;

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

        public Builder origemNome(String origemNome) {
            this.origemNome = origemNome;
            return this;
        }

        public Builder origemLatitude(Double origemLatitude) {
            this.origemLatitude = origemLatitude;
            return this;
        }

        public Builder origemLongitude(Double origemLongitude) {
            this.origemLongitude = origemLongitude;
            return this;
        }

        public Builder origemEndereco(String origemEndereco) {
            this.origemEndereco = origemEndereco;
            return this;
        }

        public Builder origemCep(String origemCep) {
            this.origemCep = origemCep;
            return this;
        }

        public Builder destinoNome(String destinoNome) {
            this.destinoNome = destinoNome;
            return this;
        }

        public Builder destinoLatitude(Double destinoLatitude) {
            this.destinoLatitude = destinoLatitude;
            return this;
        }

        public Builder destinoLongitude(Double destinoLongitude) {
            this.destinoLongitude = destinoLongitude;
            return this;
        }

        public Builder destinoEndereco(String destinoEndereco) {
            this.destinoEndereco = destinoEndereco;
            return this;
        }

        public Builder destinoCep(String destinoCep) {
            this.destinoCep = destinoCep;
            return this;
        }

        public Builder distanciaKm(Double distanciaKm) {
            this.distanciaKm = distanciaKm;
            return this;
        }

        public Builder tempoEstimadoMin(Integer tempoEstimadoMin) {
            this.tempoEstimadoMin = tempoEstimadoMin;
            return this;
        }

        public Builder polylineEncoded(String polylineEncoded) {
            this.polylineEncoded = polylineEncoded;
            return this;
        }

        public Builder rotaGeojson(Object rotaGeojson) {
            this.rotaGeojson = rotaGeojson;
            return this;
        }

        public Builder pontosRota(List<CoordenadasDto> pontosRota) {
            this.pontosRota = pontosRota;
            return this;
        }

        public Builder pontosSnap(List<CoordenadasDto> pontosSnap) {
            this.pontosSnap = pontosSnap;
            return this;
        }

        public Builder osrmRequestId(String osrmRequestId) {
            this.osrmRequestId = osrmRequestId;
            return this;
        }

        public Builder paradas(List<ParadaDto> paradas) {
            this.paradas = paradas;
            return this;
        }

        public Builder sequenciaOtimizada(List<ParadaDto> sequenciaOtimizada) {
            this.sequenciaOtimizada = sequenciaOtimizada;
            return this;
        }

        public Builder totalParadas(Integer totalParadas) {
            this.totalParadas = totalParadas;
            return this;
        }

        public Builder custoPedagioEst(BigDecimal custoPedagioEst) {
            this.custoPedagioEst = custoPedagioEst;
            return this;
        }

        public Builder custoCombustivelEst(BigDecimal custoCombustivelEst) {
            this.custoCombustivelEst = custoCombustivelEst;
            return this;
        }

        public Builder custoTotalEst(BigDecimal custoTotalEst) {
            this.custoTotalEst = custoTotalEst;
            return this;
        }

        public Builder toleranciaDesvioM(Double toleranciaDesvioM) {
            this.toleranciaDesvioM = toleranciaDesvioM;
            return this;
        }

        public Builder thresholdAlertaM(Double thresholdAlertaM) {
            this.thresholdAlertaM = thresholdAlertaM;
            return this;
        }

        public Builder maxKmExtrasAlerta(Double maxKmExtrasAlerta) {
            this.maxKmExtrasAlerta = maxKmExtrasAlerta;
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

        public Builder motoristaUuid(String motoristaUuid) {
            this.motoristaUuid = motoristaUuid;
            return this;
        }

        public Builder cargaUuid(String cargaUuid) {
            this.cargaUuid = cargaUuid;
            return this;
        }

        public Builder dataInicioPlan(LocalDateTime dataInicioPlan) {
            this.dataInicioPlan = dataInicioPlan;
            return this;
        }

        public Builder dataFimPlan(LocalDateTime dataFimPlan) {
            this.dataFimPlan = dataFimPlan;
            return this;
        }

        public Builder dataInicioReal(LocalDateTime dataInicioReal) {
            this.dataInicioReal = dataInicioReal;
            return this;
        }

        public Builder dataFimReal(LocalDateTime dataFimReal) {
            this.dataFimReal = dataFimReal;
            return this;
        }

        public Builder mdfeChave(String mdfeChave) {
            this.mdfeChave = mdfeChave;
            return this;
        }

        public Builder cteChave(String cteChave) {
            this.cteChave = cteChave;
            return this;
        }

        public Builder ciotCodigo(String ciotCodigo) {
            this.ciotCodigo = ciotCodigo;
            return this;
        }

        public Builder perfilOsrm(PerfilOsrm perfilOsrm) {
            this.perfilOsrm = perfilOsrm;
            return this;
        }

        public Builder status(StatusRota status) {
            this.status = status;
            return this;
        }

        public Builder motivoCancelamento(String motivoCancelamento) {
            this.motivoCancelamento = motivoCancelamento;
            return this;
        }

        public Builder ativa(Boolean ativa) {
            this.ativa = ativa;
            return this;
        }

        public Builder criadoPor(String criadoPor) {
            this.criadoPor = criadoPor;
            return this;
        }

        public Builder criadoEm(LocalDateTime criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Builder atualizadoEm(LocalDateTime atualizadoEm) {
            this.atualizadoEm = atualizadoEm;
            return this;
        }

        public Builder veiculo(VeiculoCache veiculo) {
            this.veiculo = veiculo;
            return this;
        }

        public Builder motorista(MotoristaCache motorista) {
            this.motorista = motorista;
            return this;
        }

        public Builder viagens(List<Viagem> viagens) {
            this.viagens = viagens;
            return this;
        }

        public Rota build() {
            return new Rota(
                    id, uuid, tenantId, nome,
                    origemNome, origemLatitude, origemLongitude,
                    origemEndereco, origemCep,
                    destinoNome, destinoLatitude, destinoLongitude,
                    destinoEndereco, destinoCep,
                    distanciaKm, tempoEstimadoMin, polylineEncoded,
                    rotaGeojson, pontosRota, pontosSnap,
                    osrmRequestId, paradas, sequenciaOtimizada,
                    totalParadas, custoPedagioEst, custoCombustivelEst,
                    custoTotalEst, toleranciaDesvioM, thresholdAlertaM,
                    maxKmExtrasAlerta, veiculoId, veiculoUuid, motoristaId,
                    motoristaUuid, cargaUuid, dataInicioPlan,
                    dataFimPlan, dataInicioReal, dataFimReal,
                    mdfeChave, cteChave, ciotCodigo, perfilOsrm,
                    status, motivoCancelamento, ativa, criadoPor,
                    criadoEm, atualizadoEm, veiculo, motorista, viagens);
        }
    }

    // ==================== CLASSES INTERNAS ====================

    public static class CoordenadasDto {
        private Double lat;
        private Double lng;

        public CoordenadasDto() {
        }

        public CoordenadasDto(Double lat, Double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }
    }

    public static class ParadaDto {
        private Integer seq;
        private String nome;
        private Double lat;
        private Double lng;
        private String janelasInicio; // HH:mm
        private String janelaFim; // HH:mm
        private String tipo; // ENTREGA, COLETA

        public ParadaDto() {
        }

        public ParadaDto(Integer seq, String nome, Double lat, Double lng,
                String janelasInicio, String janelaFim, String tipo) {
            this.seq = seq;
            this.nome = nome;
            this.lat = lat;
            this.lng = lng;
            this.janelasInicio = janelasInicio;
            this.janelaFim = janelaFim;
            this.tipo = tipo;
        }

        public Integer getSeq() {
            return seq;
        }

        public void setSeq(Integer seq) {
            this.seq = seq;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }

        public String getJanelasInicio() {
            return janelasInicio;
        }

        public void setJanelasInicio(String janelasInicio) {
            this.janelasInicio = janelasInicio;
        }

        public String getJanelaFim() {
            return janelaFim;
        }

        public void setJanelaFim(String janelaFim) {
            this.janelaFim = janelaFim;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }
    }
}