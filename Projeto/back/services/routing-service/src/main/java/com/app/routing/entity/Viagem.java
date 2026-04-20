package com.app.routing.entity;

import com.app.routing.enums.StatusViagem;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "viagens", indexes = {
        @Index(name = "idx_viagem_tenant", columnList = "tenant_id"),
        @Index(name = "idx_viagem_rota", columnList = "rota_id"),
        @Index(name = "idx_viagem_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_viagem_motorista", columnList = "motorista_id"),
        @Index(name = "idx_viagem_status", columnList = "status"),
        @Index(name = "idx_viagem_datas", columnList = "data_saida_real, data_chegada_real"),
        @Index(name = "idx_viagem_uuid", columnList = "uuid")
})
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "rota_id")
    private Long rotaId;

    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "veiculo_uuid", nullable = false, length = 36)
    private String veiculoUuid;

    @Column(name = "motorista_id")
    private Long motoristaId;

    @Column(name = "motorista_uuid", length = 36)
    private String motoristaUuid;

    @Column(name = "carga_uuid", length = 36)
    private String cargaUuid;

    // ── Datas ─────────────────────────────────────────────────
    @Column(name = "data_saida_plan")
    private LocalDateTime dataSaidaPlan;

    @Column(name = "data_saida_real")
    private LocalDateTime dataSaidaReal;

    @Column(name = "data_chegada_plan")
    private LocalDateTime dataChegadaPlan;

    @Column(name = "data_chegada_real")
    private LocalDateTime dataChegadaReal;

    @Column(name = "eta_atual")
    private LocalDateTime etaAtual;

    @Column(name = "eta_atualizado_em")
    private LocalDateTime etaAtualizadoEm;

    // ── Métricas ──────────────────────────────────────────────
    @Column(name = "distancia_real_km", nullable = false)
    private Double distanciaRealKm;

    @Column(name = "consumo_real_l", nullable = false)
    private Double consumoRealL;

    @Column(name = "km_fora_rota", nullable = false)
    private Double kmForaRota;

    @Column(name = "velocidade_media")
    private Double velocidadeMedia;

    @Column(name = "velocidade_maxima")
    private Double velocidadeMaxima;

    // ── Última posição ─────────────────────────────────────────
    @Column(name = "ultima_lat")
    private Double ultimaLat;

    @Column(name = "ultima_lng")
    private Double ultimaLng;

    @Column(name = "ultima_posicao_em")
    private LocalDateTime ultimaPosicaoEm;

    // ── Resumo ────────────────────────────────────────────────
    @Column(name = "total_desvios", nullable = false)
    private Integer totalDesvios;

    @Column(name = "total_alertas", nullable = false)
    private Integer totalAlertas;

    @Column(name = "total_paradas", nullable = false)
    private Integer totalParadas;

    @Column(name = "tempo_ocioso_min", nullable = false)
    private Integer tempoOciosoMin;

    @Column(name = "frenagens_bruscas", nullable = false)
    private Integer frenagensBruscas;

    @Column(name = "excessos_velocidade", nullable = false)
    private Integer excessosVelocidade;

    @Column(name = "score_viagem", nullable = false)
    private Integer scoreViagem;

    // ── Status ────────────────────────────────────────────────
    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    private StatusViagem status;

    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Relacionamentos ───────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rota_id", insertable = false, updatable = false)
    private Rota rota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", insertable = false, updatable = false)
    private VeiculoCache veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", insertable = false, updatable = false)
    private MotoristaCache motorista;

    @OneToMany(mappedBy = "viagem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DesvioRota> desvios;

    @OneToMany(mappedBy = "viagem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PontoEntrega> pontosEntrega;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão (sem argumentos).
     * Inicializa os campos com os valores padrão definidos nos @Builder.Default
     * originais.
     */
    public Viagem() {
        this.uuid = UUID.randomUUID().toString();
        this.distanciaRealKm = 0.0;
        this.consumoRealL = 0.0;
        this.kmForaRota = 0.0;
        this.totalDesvios = 0;
        this.totalAlertas = 0;
        this.totalParadas = 0;
        this.tempoOciosoMin = 0;
        this.frenagensBruscas = 0;
        this.excessosVelocidade = 0;
        this.scoreViagem = 1000;
        this.status = StatusViagem.PLANEJADA;
    }

    /**
     * Construtor privado com todos os campos.
     * Usado internamente pelo Builder.
     */
    private Viagem(Long id, String uuid, Long tenantId, Long rotaId,
            Long veiculoId, String veiculoUuid, Long motoristaId,
            String motoristaUuid, String cargaUuid,
            LocalDateTime dataSaidaPlan, LocalDateTime dataSaidaReal,
            LocalDateTime dataChegadaPlan, LocalDateTime dataChegadaReal,
            LocalDateTime etaAtual, LocalDateTime etaAtualizadoEm,
            Double distanciaRealKm, Double consumoRealL, Double kmForaRota,
            Double velocidadeMedia, Double velocidadeMaxima,
            Double ultimaLat, Double ultimaLng, LocalDateTime ultimaPosicaoEm,
            Integer totalDesvios, Integer totalAlertas, Integer totalParadas,
            Integer tempoOciosoMin, Integer frenagensBruscas, Integer excessosVelocidade,
            Integer scoreViagem, StatusViagem status, String motivoCancelamento,
            LocalDateTime criadoEm, LocalDateTime atualizadoEm,
            Rota rota, VeiculoCache veiculo, MotoristaCache motorista,
            List<DesvioRota> desvios, List<PontoEntrega> pontosEntrega) {
        this.id = id;
        this.uuid = uuid != null ? uuid : UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.rotaId = rotaId;
        this.veiculoId = veiculoId;
        this.veiculoUuid = veiculoUuid;
        this.motoristaId = motoristaId;
        this.motoristaUuid = motoristaUuid;
        this.cargaUuid = cargaUuid;
        this.dataSaidaPlan = dataSaidaPlan;
        this.dataSaidaReal = dataSaidaReal;
        this.dataChegadaPlan = dataChegadaPlan;
        this.dataChegadaReal = dataChegadaReal;
        this.etaAtual = etaAtual;
        this.etaAtualizadoEm = etaAtualizadoEm;
        this.distanciaRealKm = distanciaRealKm != null ? distanciaRealKm : 0.0;
        this.consumoRealL = consumoRealL != null ? consumoRealL : 0.0;
        this.kmForaRota = kmForaRota != null ? kmForaRota : 0.0;
        this.velocidadeMedia = velocidadeMedia;
        this.velocidadeMaxima = velocidadeMaxima;
        this.ultimaLat = ultimaLat;
        this.ultimaLng = ultimaLng;
        this.ultimaPosicaoEm = ultimaPosicaoEm;
        this.totalDesvios = totalDesvios != null ? totalDesvios : 0;
        this.totalAlertas = totalAlertas != null ? totalAlertas : 0;
        this.totalParadas = totalParadas != null ? totalParadas : 0;
        this.tempoOciosoMin = tempoOciosoMin != null ? tempoOciosoMin : 0;
        this.frenagensBruscas = frenagensBruscas != null ? frenagensBruscas : 0;
        this.excessosVelocidade = excessosVelocidade != null ? excessosVelocidade : 0;
        this.scoreViagem = scoreViagem != null ? scoreViagem : 1000;
        this.status = status != null ? status : StatusViagem.PLANEJADA;
        this.motivoCancelamento = motivoCancelamento;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.rota = rota;
        this.veiculo = veiculo;
        this.motorista = motorista;
        this.desvios = desvios;
        this.pontosEntrega = pontosEntrega;
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

    public Long getRotaId() {
        return rotaId;
    }

    public void setRotaId(Long rotaId) {
        this.rotaId = rotaId;
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

    public LocalDateTime getDataSaidaPlan() {
        return dataSaidaPlan;
    }

    public void setDataSaidaPlan(LocalDateTime dataSaidaPlan) {
        this.dataSaidaPlan = dataSaidaPlan;
    }

    public LocalDateTime getDataSaidaReal() {
        return dataSaidaReal;
    }

    public void setDataSaidaReal(LocalDateTime dataSaidaReal) {
        this.dataSaidaReal = dataSaidaReal;
    }

    public LocalDateTime getDataChegadaPlan() {
        return dataChegadaPlan;
    }

    public void setDataChegadaPlan(LocalDateTime dataChegadaPlan) {
        this.dataChegadaPlan = dataChegadaPlan;
    }

    public LocalDateTime getDataChegadaReal() {
        return dataChegadaReal;
    }

    public void setDataChegadaReal(LocalDateTime dataChegadaReal) {
        this.dataChegadaReal = dataChegadaReal;
    }

    public LocalDateTime getEtaAtual() {
        return etaAtual;
    }

    public void setEtaAtual(LocalDateTime etaAtual) {
        this.etaAtual = etaAtual;
    }

    public LocalDateTime getEtaAtualizadoEm() {
        return etaAtualizadoEm;
    }

    public void setEtaAtualizadoEm(LocalDateTime etaAtualizadoEm) {
        this.etaAtualizadoEm = etaAtualizadoEm;
    }

    public Double getDistanciaRealKm() {
        return distanciaRealKm;
    }

    public void setDistanciaRealKm(Double distanciaRealKm) {
        this.distanciaRealKm = distanciaRealKm;
    }

    public Double getConsumoRealL() {
        return consumoRealL;
    }

    public void setConsumoRealL(Double consumoRealL) {
        this.consumoRealL = consumoRealL;
    }

    public Double getKmForaRota() {
        return kmForaRota;
    }

    public void setKmForaRota(Double kmForaRota) {
        this.kmForaRota = kmForaRota;
    }

    public Double getVelocidadeMedia() {
        return velocidadeMedia;
    }

    public void setVelocidadeMedia(Double velocidadeMedia) {
        this.velocidadeMedia = velocidadeMedia;
    }

    public Double getVelocidadeMaxima() {
        return velocidadeMaxima;
    }

    public void setVelocidadeMaxima(Double velocidadeMaxima) {
        this.velocidadeMaxima = velocidadeMaxima;
    }

    public Double getUltimaLat() {
        return ultimaLat;
    }

    public void setUltimaLat(Double ultimaLat) {
        this.ultimaLat = ultimaLat;
    }

    public Double getUltimaLng() {
        return ultimaLng;
    }

    public void setUltimaLng(Double ultimaLng) {
        this.ultimaLng = ultimaLng;
    }

    public LocalDateTime getUltimaPosicaoEm() {
        return ultimaPosicaoEm;
    }

    public void setUltimaPosicaoEm(LocalDateTime ultimaPosicaoEm) {
        this.ultimaPosicaoEm = ultimaPosicaoEm;
    }

    public Integer getTotalDesvios() {
        return totalDesvios;
    }

    public void setTotalDesvios(Integer totalDesvios) {
        this.totalDesvios = totalDesvios;
    }

    public Integer getTotalAlertas() {
        return totalAlertas;
    }

    public void setTotalAlertas(Integer totalAlertas) {
        this.totalAlertas = totalAlertas;
    }

    public Integer getTotalParadas() {
        return totalParadas;
    }

    public void setTotalParadas(Integer totalParadas) {
        this.totalParadas = totalParadas;
    }

    public Integer getTempoOciosoMin() {
        return tempoOciosoMin;
    }

    public void setTempoOciosoMin(Integer tempoOciosoMin) {
        this.tempoOciosoMin = tempoOciosoMin;
    }

    public Integer getFrenagensBruscas() {
        return frenagensBruscas;
    }

    public void setFrenagensBruscas(Integer frenagensBruscas) {
        this.frenagensBruscas = frenagensBruscas;
    }

    public Integer getExcessosVelocidade() {
        return excessosVelocidade;
    }

    public void setExcessosVelocidade(Integer excessosVelocidade) {
        this.excessosVelocidade = excessosVelocidade;
    }

    public Integer getScoreViagem() {
        return scoreViagem;
    }

    public void setScoreViagem(Integer scoreViagem) {
        this.scoreViagem = scoreViagem;
    }

    public StatusViagem getStatus() {
        return status;
    }

    public void setStatus(StatusViagem status) {
        this.status = status;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
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

    public Rota getRota() {
        return rota;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
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

    public List<DesvioRota> getDesvios() {
        return desvios;
    }

    public void setDesvios(List<DesvioRota> desvios) {
        this.desvios = desvios;
    }

    public List<PontoEntrega> getPontosEntrega() {
        return pontosEntrega;
    }

    public void setPontosEntrega(List<PontoEntrega> pontosEntrega) {
        this.pontosEntrega = pontosEntrega;
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String uuid = UUID.randomUUID().toString();
        private Long tenantId;
        private Long rotaId;
        private Long veiculoId;
        private String veiculoUuid;
        private Long motoristaId;
        private String motoristaUuid;
        private String cargaUuid;
        private LocalDateTime dataSaidaPlan;
        private LocalDateTime dataSaidaReal;
        private LocalDateTime dataChegadaPlan;
        private LocalDateTime dataChegadaReal;
        private LocalDateTime etaAtual;
        private LocalDateTime etaAtualizadoEm;
        private Double distanciaRealKm = 0.0;
        private Double consumoRealL = 0.0;
        private Double kmForaRota = 0.0;
        private Double velocidadeMedia;
        private Double velocidadeMaxima;
        private Double ultimaLat;
        private Double ultimaLng;
        private LocalDateTime ultimaPosicaoEm;
        private Integer totalDesvios = 0;
        private Integer totalAlertas = 0;
        private Integer totalParadas = 0;
        private Integer tempoOciosoMin = 0;
        private Integer frenagensBruscas = 0;
        private Integer excessosVelocidade = 0;
        private Integer scoreViagem = 1000;
        private StatusViagem status = StatusViagem.PLANEJADA;
        private String motivoCancelamento;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;
        private Rota rota;
        private VeiculoCache veiculo;
        private MotoristaCache motorista;
        private List<DesvioRota> desvios;
        private List<PontoEntrega> pontosEntrega;

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

        public Builder rotaId(Long rotaId) {
            this.rotaId = rotaId;
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

        public Builder dataSaidaPlan(LocalDateTime dataSaidaPlan) {
            this.dataSaidaPlan = dataSaidaPlan;
            return this;
        }

        public Builder dataSaidaReal(LocalDateTime dataSaidaReal) {
            this.dataSaidaReal = dataSaidaReal;
            return this;
        }

        public Builder dataChegadaPlan(LocalDateTime dataChegadaPlan) {
            this.dataChegadaPlan = dataChegadaPlan;
            return this;
        }

        public Builder dataChegadaReal(LocalDateTime dataChegadaReal) {
            this.dataChegadaReal = dataChegadaReal;
            return this;
        }

        public Builder etaAtual(LocalDateTime etaAtual) {
            this.etaAtual = etaAtual;
            return this;
        }

        public Builder etaAtualizadoEm(LocalDateTime etaAtualizadoEm) {
            this.etaAtualizadoEm = etaAtualizadoEm;
            return this;
        }

        public Builder distanciaRealKm(Double distanciaRealKm) {
            this.distanciaRealKm = distanciaRealKm;
            return this;
        }

        public Builder consumoRealL(Double consumoRealL) {
            this.consumoRealL = consumoRealL;
            return this;
        }

        public Builder kmForaRota(Double kmForaRota) {
            this.kmForaRota = kmForaRota;
            return this;
        }

        public Builder velocidadeMedia(Double velocidadeMedia) {
            this.velocidadeMedia = velocidadeMedia;
            return this;
        }

        public Builder velocidadeMaxima(Double velocidadeMaxima) {
            this.velocidadeMaxima = velocidadeMaxima;
            return this;
        }

        public Builder ultimaLat(Double ultimaLat) {
            this.ultimaLat = ultimaLat;
            return this;
        }

        public Builder ultimaLng(Double ultimaLng) {
            this.ultimaLng = ultimaLng;
            return this;
        }

        public Builder ultimaPosicaoEm(LocalDateTime ultimaPosicaoEm) {
            this.ultimaPosicaoEm = ultimaPosicaoEm;
            return this;
        }

        public Builder totalDesvios(Integer totalDesvios) {
            this.totalDesvios = totalDesvios;
            return this;
        }

        public Builder totalAlertas(Integer totalAlertas) {
            this.totalAlertas = totalAlertas;
            return this;
        }

        public Builder totalParadas(Integer totalParadas) {
            this.totalParadas = totalParadas;
            return this;
        }

        public Builder tempoOciosoMin(Integer tempoOciosoMin) {
            this.tempoOciosoMin = tempoOciosoMin;
            return this;
        }

        public Builder frenagensBruscas(Integer frenagensBruscas) {
            this.frenagensBruscas = frenagensBruscas;
            return this;
        }

        public Builder excessosVelocidade(Integer excessosVelocidade) {
            this.excessosVelocidade = excessosVelocidade;
            return this;
        }

        public Builder scoreViagem(Integer scoreViagem) {
            this.scoreViagem = scoreViagem;
            return this;
        }

        public Builder status(StatusViagem status) {
            this.status = status;
            return this;
        }

        public Builder motivoCancelamento(String motivoCancelamento) {
            this.motivoCancelamento = motivoCancelamento;
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

        public Builder rota(Rota rota) {
            this.rota = rota;
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

        public Builder desvios(List<DesvioRota> desvios) {
            this.desvios = desvios;
            return this;
        }

        public Builder pontosEntrega(List<PontoEntrega> pontosEntrega) {
            this.pontosEntrega = pontosEntrega;
            return this;
        }

        public Viagem build() {
            return new Viagem(
                    id, uuid, tenantId, rotaId,
                    veiculoId, veiculoUuid, motoristaId,
                    motoristaUuid, cargaUuid,
                    dataSaidaPlan, dataSaidaReal,
                    dataChegadaPlan, dataChegadaReal,
                    etaAtual, etaAtualizadoEm,
                    distanciaRealKm, consumoRealL, kmForaRota,
                    velocidadeMedia, velocidadeMaxima,
                    ultimaLat, ultimaLng, ultimaPosicaoEm,
                    totalDesvios, totalAlertas, totalParadas,
                    tempoOciosoMin, frenagensBruscas, excessosVelocidade,
                    scoreViagem, status, motivoCancelamento,
                    criadoEm, atualizadoEm,
                    rota, veiculo, motorista,
                    desvios, pontosEntrega);
        }
    }
}