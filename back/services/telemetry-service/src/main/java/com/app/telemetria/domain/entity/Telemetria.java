package com.app.telemetria.domain.entity;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "telemetria", indexes = {
        @Index(name = "idx_tel_veiculo_data", columnList = "veiculo_id, data_hora DESC"),
        @Index(name = "idx_tel_tenant_data", columnList = "tenant_id, data_hora DESC"),
        @Index(name = "idx_tel_viagem", columnList = "viagem_id"),
        @Index(name = "idx_tel_device", columnList = "device_id"),
        @Index(name = "idx_tel_data_hora", columnList = "data_hora"),
        @Index(name = "idx_tel_eventos", columnList = "colisao_detectada, botao_panico, adulteracao_gps"),
        @Index(name = "idx_tel_alertas", columnList = "excesso_velocidade, frenagem_brusca, geofence_violada"),
        @Index(name = "idx_tel_localizacao", columnList = "latitude, longitude"),
        @Index(name = "idx_tel_dms", columnList = "fadiga_detectada, uso_celular_detectado")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Telemetria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identificação ──────────────────────────────────────────
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(name = "veiculo_uuid", nullable = false, length = 36)
    private String veiculoUuid;

    @Column(name = "motorista_id")
    private Long motoristaId;

    @Column(name = "viagem_id")
    private Long viagemId;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "imei_dispositivo", length = 20)
    private String imeiDispositivo;

    // ── GPS ────────────────────────────────────────────────────
    @Column(name = "latitude", nullable = false)
    private Double latitude;
    @Column(name = "longitude", nullable = false)
    private Double longitude;
    @Column(name = "altitude")
    private Double altitude;
    @Column(name = "velocidade", nullable = false)
    private Double velocidade = 0.0;
    @Column(name = "direcao")
    private Double direcao;
    @Column(name = "hdop")
    private Double hdop;
    @Column(name = "satelites")
    private Integer satelites;
    @Column(name = "precisao_gps")
    private Double precisaoGps;
    @Column(name = "lat_snap")
    private Double latSnap;
    @Column(name = "lng_snap")
    private Double lngSnap;
    @Column(name = "nome_via")
    private String nomeVia;

    // ── Motor / OBD-II ─────────────────────────────────────────
    @Column(name = "ignicao", nullable = false)
    private Boolean ignicao = false;
    @Column(name = "rpm", nullable = false)
    private Double rpm = 0.0;
    @Column(name = "carga_motor")
    private Double cargaMotor;
    @Column(name = "torque_motor")
    private Double torqueMotor;
    @Column(name = "temperatura_motor")
    private Double temperaturaMotor;
    @Column(name = "pressao_oleo")
    private Double pressaoOleo;
    @Column(name = "tensao_bateria")
    private Double tensaoBateria;
    @Column(name = "odometro", nullable = false)
    private Double odometro = 0.0;
    @Column(name = "horas_motor")
    private Double horasMotor;
    @Column(name = "aceleracao")
    private Double aceleracao;
    @Column(name = "inclinacao")
    private Double inclinacao;

    // ── Combustível ────────────────────────────────────────────
    @Column(name = "nivel_combustivel")
    private Double nivelCombustivel;
    @Column(name = "consumo_combustivel")
    private Double consumoCombustivel;
    @Column(name = "consumo_acumulado")
    private Double consumoAcumulado;
    @Column(name = "tempo_ocioso")
    private Integer tempoOcioso;
    @Column(name = "tempo_motor_ligado")
    private Integer tempoMotorLigado;

    // ── Comportamento ──────────────────────────────────────────
    @Column(name = "frenagem_brusca", nullable = false)
    private Boolean frenagemBrusca = false;
    @Column(name = "numero_frenagens", nullable = false)
    private Integer numeroFrenagens = 0;
    @Column(name = "numero_aceleracoes_bruscas", nullable = false)
    private Integer numeroAceleracoesBruscas = 0;
    @Column(name = "excesso_velocidade", nullable = false)
    private Boolean excessoVelocidade = false;
    @Column(name = "velocidade_limite_via")
    private Double velocidadeLimiteVia;
    @Column(name = "curva_brusca", nullable = false)
    private Boolean curvaBrusca = false;
    @Column(name = "pontuacao_motorista")
    private Integer pontuacaoMotorista;

    // ── Segurança ──────────────────────────────────────────────
    @Column(name = "colisao_detectada", nullable = false)
    private Boolean colisaoDetectada = false;
    @Column(name = "geofence_violada", nullable = false)
    private Boolean geofenceViolada = false;
    @Column(name = "geofence_id")
    private Long geofenceId;
    @Column(name = "cinto_seguranca", nullable = false)
    private Boolean cintoSeguranca = true;
    @Column(name = "porta_aberta", nullable = false)
    private Boolean portaAberta = false;
    @Column(name = "botao_panico", nullable = false)
    private Boolean botaoPanico = false;
    @Column(name = "adulteracao_gps", nullable = false)
    private Boolean adulteracaoGps = false;

    // ── Carga ──────────────────────────────────────────────────
    @Column(name = "temperatura_carga")
    private Double temperaturaCarga;
    @Column(name = "umidade_carga")
    private Double umidadeCarga;
    @Column(name = "peso_carga_kg")
    private Double pesoCargaKg;
    @Column(name = "porta_bau_aberta", nullable = false)
    private Boolean portaBauAberta = false;
    @Column(name = "impacto_carga", nullable = false)
    private Boolean impactoCarga = false;
    @Column(name = "g_force_impacto")
    private Double gForceImpacto;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pressao_pneus_json", columnDefinition = "json")
    private Object pressaoPneusJson;

    @Column(name = "alerta_pneu", nullable = false)
    private Boolean alertaPneu = false;

    // ── DMS ────────────────────────────────────────────────────
    @Column(name = "fadiga_detectada", nullable = false)
    private Boolean fadigaDetectada = false;
    @Column(name = "distracao_detectada", nullable = false)
    private Boolean distracaoDetectada = false;
    @Column(name = "uso_celular_detectado", nullable = false)
    private Boolean usoCelularDetectado = false;
    @Column(name = "cigarro_detectado", nullable = false)
    private Boolean cigarroDetectado = false;
    @Column(name = "ausencia_cinto_dms", nullable = false)
    private Boolean ausenciaCintoDms = false;
    @Column(name = "score_dms")
    private Integer scoreDms;

    // ── Ambiente ───────────────────────────────────────────────
    @Column(name = "temperatura_externa")
    private Double temperaturaExterna;
    @Column(name = "umidade_externa")
    private Double umidadeExterna;
    @Column(name = "chuva_detectada", nullable = false)
    private Boolean chuvaDetectada = false;
    @Column(name = "condicao_pista", length = 30)
    private String condicaoPista;

    // ── Conectividade ──────────────────────────────────────────
    @Column(name = "sinal_gsm")
    private Double sinalGsm;
    @Column(name = "sinal_gps")
    private Double sinalGps;
    @Column(name = "tecnologia_rede", length = 10)
    private String tecnologiaRede;
    @Column(name = "firmware_versao", length = 30)
    private String firmwareVersao;
    @Column(name = "modo_offline", nullable = false)
    private Boolean modoOffline = false;
    @Column(name = "delay_sincronizacao_s")
    private Integer delaySincronizacaoS;

    // ── Tacógrafo ──────────────────────────────────────────────
    @Column(name = "tacografo_status", length = 20)
    private String tacografoStatus;
    @Column(name = "tacografo_velocidade")
    private Double tacografoVelocidade;
    @Column(name = "tacografo_distancia")
    private Double tacografoDistancia;
    @Column(name = "horas_direcao_acumuladas")
    private Double horasDirecaoAcumuladas;

    // ── Manutenção ─────────────────────────────────────────────
    @Column(name = "manutencao_pendente", nullable = false)
    private Boolean manutencaoPendente = false;
    @Column(name = "proxima_revisao")
    private LocalDateTime proximaRevisao;
    @Column(name = "desgaste_freio")
    private Double desgasteFreio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dtc_codes", columnDefinition = "json")
    private Object dtcCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "json")
    private Map<String, Object> payload;

    // ── Timestamps ─────────────────────────────────────────────
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @CreationTimestamp
    @Column(name = "recebido_em", nullable = false, updatable = false)
    private LocalDateTime recebidoEm;

    @Column(name = "processado_em")
    private LocalDateTime processadoEm;

    // ── Relacionamentos JPA (objetos completos) ────────────────

    /**
     * Veículo de origem dos dados (via FK veiculo_id → veiculos).
     * Inserção/update controlados pelas colunas acima.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", insertable = false, updatable = false)
    @JsonIgnore
    private Veiculo veiculo;

    /**
     * Motorista que estava dirigindo no momento do evento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", insertable = false, updatable = false)
    @JsonIgnore
    private Motorista motorista;

    /**
     * Viagem associada a este evento de telemetria (opcional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    @JsonIgnore
    private Viagem viagem;

    // ── Construtores ───────────────────────────────────────────

    public Telemetria() {
        this.velocidade = 0.0;
        this.ignicao = false;
        this.rpm = 0.0;
        this.odometro = 0.0;
        this.frenagemBrusca = false;
        this.numeroFrenagens = 0;
        this.numeroAceleracoesBruscas = 0;
        this.excessoVelocidade = false;
        this.curvaBrusca = false;
        this.colisaoDetectada = false;
        this.geofenceViolada = false;
        this.cintoSeguranca = true;
        this.portaAberta = false;
        this.botaoPanico = false;
        this.adulteracaoGps = false;
        this.portaBauAberta = false;
        this.impactoCarga = false;
        this.alertaPneu = false;
        this.fadigaDetectada = false;
        this.distracaoDetectada = false;
        this.usoCelularDetectado = false;
        this.cigarroDetectado = false;
        this.ausenciaCintoDms = false;
        this.chuvaDetectada = false;
        this.modoOffline = false;
        this.manutencaoPendente = false;
    }

    // ── Builder ────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long tenantId;
        private Long veiculoId;
        private String veiculoUuid;
        private Long motoristaId;
        private Long viagemId;
        private String deviceId;
        private Double latitude;
        private Double longitude;
        private Double velocidade = 0.0;
        private Boolean ignicao = false;
        private Double rpm = 0.0;
        private Double odometro = 0.0;
        private Boolean frenagemBrusca = false;
        private Integer numeroFrenagens = 0;
        private Integer numeroAceleracoesBruscas = 0;
        private Boolean excessoVelocidade = false;
        private Boolean curvaBrusca = false;
        private Boolean colisaoDetectada = false;
        private Boolean geofenceViolada = false;
        private Boolean cintoSeguranca = true;
        private Boolean portaAberta = false;
        private Boolean botaoPanico = false;
        private Boolean adulteracaoGps = false;
        private Boolean portaBauAberta = false;
        private Boolean impactoCarga = false;
        private Boolean alertaPneu = false;
        private Boolean fadigaDetectada = false;
        private Boolean distracaoDetectada = false;
        private Boolean usoCelularDetectado = false;
        private Boolean cigarroDetectado = false;
        private Boolean ausenciaCintoDms = false;
        private Boolean chuvaDetectada = false;
        private Boolean modoOffline = false;
        private Boolean manutencaoPendente = false;
        private LocalDateTime dataHora;

        private Builder() {
        }

        public Builder tenantId(Long v) {
            this.tenantId = v;
            return this;
        }

        public Builder veiculoId(Long v) {
            this.veiculoId = v;
            return this;
        }

        public Builder veiculoUuid(String v) {
            this.veiculoUuid = v;
            return this;
        }

        public Builder motoristaId(Long v) {
            this.motoristaId = v;
            return this;
        }

        public Builder viagemId(Long v) {
            this.viagemId = v;
            return this;
        }

        public Builder deviceId(String v) {
            this.deviceId = v;
            return this;
        }

        public Builder latitude(Double v) {
            this.latitude = v;
            return this;
        }

        public Builder longitude(Double v) {
            this.longitude = v;
            return this;
        }

        public Builder velocidade(Double v) {
            this.velocidade = v;
            return this;
        }

        public Builder ignicao(Boolean v) {
            this.ignicao = v;
            return this;
        }

        public Builder rpm(Double v) {
            this.rpm = v;
            return this;
        }

        public Builder odometro(Double v) {
            this.odometro = v;
            return this;
        }

        public Builder frenagemBrusca(Boolean v) {
            this.frenagemBrusca = v;
            return this;
        }

        public Builder excessoVelocidade(Boolean v) {
            this.excessoVelocidade = v;
            return this;
        }

        public Builder botaoPanico(Boolean v) {
            this.botaoPanico = v;
            return this;
        }

        public Builder colisaoDetectada(Boolean v) {
            this.colisaoDetectada = v;
            return this;
        }

        public Builder fadigaDetectada(Boolean v) {
            this.fadigaDetectada = v;
            return this;
        }

        public Builder dataHora(LocalDateTime v) {
            this.dataHora = v;
            return this;
        }

        public Telemetria build() {
            Telemetria t = new Telemetria();
            t.tenantId = tenantId;
            t.veiculoId = veiculoId;
            t.veiculoUuid = veiculoUuid;
            t.motoristaId = motoristaId;
            t.viagemId = viagemId;
            t.deviceId = deviceId;
            t.latitude = latitude;
            t.longitude = longitude;
            t.velocidade = velocidade;
            t.ignicao = ignicao;
            t.rpm = rpm;
            t.odometro = odometro;
            t.frenagemBrusca = frenagemBrusca;
            t.excessoVelocidade = excessoVelocidade;
            t.botaoPanico = botaoPanico;
            t.colisaoDetectada = colisaoDetectada;
            t.fadigaDetectada = fadigaDetectada;
            t.dataHora = dataHora;
            t.numeroFrenagens = numeroFrenagens;
            t.curvaBrusca = curvaBrusca;
            t.geofenceViolada = geofenceViolada;
            t.cintoSeguranca = cintoSeguranca;
            t.portaAberta = portaAberta;
            t.adulteracaoGps = adulteracaoGps;
            t.portaBauAberta = portaBauAberta;
            t.impactoCarga = impactoCarga;
            t.alertaPneu = alertaPneu;
            t.distracaoDetectada = distracaoDetectada;
            t.usoCelularDetectado = usoCelularDetectado;
            t.cigarroDetectado = cigarroDetectado;
            t.ausenciaCintoDms = ausenciaCintoDms;
            t.chuvaDetectada = chuvaDetectada;
            t.modoOffline = modoOffline;
            t.manutencaoPendente = manutencaoPendente;
            t.numeroAceleracoesBruscas = numeroAceleracoesBruscas;
            return t;
        }
    }

    // ── Getters e Setters ──────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getImeiDispositivo() {
        return imeiDispositivo;
    }

    public void setImeiDispositivo(String imeiDispositivo) {
        this.imeiDispositivo = imeiDispositivo;
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

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getVelocidade() {
        return velocidade;
    }

    public void setVelocidade(Double velocidade) {
        this.velocidade = velocidade;
    }

    public Double getDirecao() {
        return direcao;
    }

    public void setDirecao(Double direcao) {
        this.direcao = direcao;
    }

    public Double getHdop() {
        return hdop;
    }

    public void setHdop(Double hdop) {
        this.hdop = hdop;
    }

    public Integer getSatelites() {
        return satelites;
    }

    public void setSatelites(Integer satelites) {
        this.satelites = satelites;
    }

    public Double getPrecisaoGps() {
        return precisaoGps;
    }

    public void setPrecisaoGps(Double precisaoGps) {
        this.precisaoGps = precisaoGps;
    }

    public Double getLatSnap() {
        return latSnap;
    }

    public void setLatSnap(Double latSnap) {
        this.latSnap = latSnap;
    }

    public Double getLngSnap() {
        return lngSnap;
    }

    public void setLngSnap(Double lngSnap) {
        this.lngSnap = lngSnap;
    }

    public String getNomeVia() {
        return nomeVia;
    }

    public void setNomeVia(String nomeVia) {
        this.nomeVia = nomeVia;
    }

    public Boolean getIgnicao() {
        return ignicao;
    }

    public void setIgnicao(Boolean ignicao) {
        this.ignicao = ignicao;
    }

    public Double getRpm() {
        return rpm;
    }

    public void setRpm(Double rpm) {
        this.rpm = rpm;
    }

    public Double getCargaMotor() {
        return cargaMotor;
    }

    public void setCargaMotor(Double cargaMotor) {
        this.cargaMotor = cargaMotor;
    }

    public Double getTorqueMotor() {
        return torqueMotor;
    }

    public void setTorqueMotor(Double torqueMotor) {
        this.torqueMotor = torqueMotor;
    }

    public Double getTemperaturaMotor() {
        return temperaturaMotor;
    }

    public void setTemperaturaMotor(Double temperaturaMotor) {
        this.temperaturaMotor = temperaturaMotor;
    }

    public Double getPressaoOleo() {
        return pressaoOleo;
    }

    public void setPressaoOleo(Double pressaoOleo) {
        this.pressaoOleo = pressaoOleo;
    }

    public Double getTensaoBateria() {
        return tensaoBateria;
    }

    public void setTensaoBateria(Double tensaoBateria) {
        this.tensaoBateria = tensaoBateria;
    }

    public Double getOdometro() {
        return odometro;
    }

    public void setOdometro(Double odometro) {
        this.odometro = odometro;
    }

    public Double getHorasMotor() {
        return horasMotor;
    }

    public void setHorasMotor(Double horasMotor) {
        this.horasMotor = horasMotor;
    }

    public Double getAceleracao() {
        return aceleracao;
    }

    public void setAceleracao(Double aceleracao) {
        this.aceleracao = aceleracao;
    }

    public Double getInclinacao() {
        return inclinacao;
    }

    public void setInclinacao(Double inclinacao) {
        this.inclinacao = inclinacao;
    }

    public Double getNivelCombustivel() {
        return nivelCombustivel;
    }

    public void setNivelCombustivel(Double nivelCombustivel) {
        this.nivelCombustivel = nivelCombustivel;
    }

    public Double getConsumoCombustivel() {
        return consumoCombustivel;
    }

    public void setConsumoCombustivel(Double consumoCombustivel) {
        this.consumoCombustivel = consumoCombustivel;
    }

    public Double getConsumoAcumulado() {
        return consumoAcumulado;
    }

    public void setConsumoAcumulado(Double consumoAcumulado) {
        this.consumoAcumulado = consumoAcumulado;
    }

    public Integer getTempoOcioso() {
        return tempoOcioso;
    }

    public void setTempoOcioso(Integer tempoOcioso) {
        this.tempoOcioso = tempoOcioso;
    }

    public Integer getTempoMotorLigado() {
        return tempoMotorLigado;
    }

    public void setTempoMotorLigado(Integer tempoMotorLigado) {
        this.tempoMotorLigado = tempoMotorLigado;
    }

    public Boolean getFrenagemBrusca() {
        return frenagemBrusca;
    }

    public void setFrenagemBrusca(Boolean frenagemBrusca) {
        this.frenagemBrusca = frenagemBrusca;
    }

    public Integer getNumeroFrenagens() {
        return numeroFrenagens;
    }

    public void setNumeroFrenagens(Integer numeroFrenagens) {
        this.numeroFrenagens = numeroFrenagens;
    }

    public Integer getNumeroAceleracoesBruscas() {
        return numeroAceleracoesBruscas;
    }

    public void setNumeroAceleracoesBruscas(Integer v) {
        this.numeroAceleracoesBruscas = v;
    }

    public Boolean getExcessoVelocidade() {
        return excessoVelocidade;
    }

    public void setExcessoVelocidade(Boolean excessoVelocidade) {
        this.excessoVelocidade = excessoVelocidade;
    }

    public Double getVelocidadeLimiteVia() {
        return velocidadeLimiteVia;
    }

    public void setVelocidadeLimiteVia(Double v) {
        this.velocidadeLimiteVia = v;
    }

    public Boolean getCurvaBrusca() {
        return curvaBrusca;
    }

    public void setCurvaBrusca(Boolean curvaBrusca) {
        this.curvaBrusca = curvaBrusca;
    }

    public Integer getPontuacaoMotorista() {
        return pontuacaoMotorista;
    }

    public void setPontuacaoMotorista(Integer pontuacaoMotorista) {
        this.pontuacaoMotorista = pontuacaoMotorista;
    }

    public Boolean getColisaoDetectada() {
        return colisaoDetectada;
    }

    public void setColisaoDetectada(Boolean colisaoDetectada) {
        this.colisaoDetectada = colisaoDetectada;
    }

    public Boolean getGeofenceViolada() {
        return geofenceViolada;
    }

    public void setGeofenceViolada(Boolean geofenceViolada) {
        this.geofenceViolada = geofenceViolada;
    }

    public Long getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(Long geofenceId) {
        this.geofenceId = geofenceId;
    }

    public Boolean getCintoSeguranca() {
        return cintoSeguranca;
    }

    public void setCintoSeguranca(Boolean cintoSeguranca) {
        this.cintoSeguranca = cintoSeguranca;
    }

    public Boolean getPortaAberta() {
        return portaAberta;
    }

    public void setPortaAberta(Boolean portaAberta) {
        this.portaAberta = portaAberta;
    }

    public Boolean getBotaoPanico() {
        return botaoPanico;
    }

    public void setBotaoPanico(Boolean botaoPanico) {
        this.botaoPanico = botaoPanico;
    }

    public Boolean getAdulteracaoGps() {
        return adulteracaoGps;
    }

    public void setAdulteracaoGps(Boolean adulteracaoGps) {
        this.adulteracaoGps = adulteracaoGps;
    }

    public Double getTemperaturaCarga() {
        return temperaturaCarga;
    }

    public void setTemperaturaCarga(Double temperaturaCarga) {
        this.temperaturaCarga = temperaturaCarga;
    }

    public Double getUmidadeCarga() {
        return umidadeCarga;
    }

    public void setUmidadeCarga(Double umidadeCarga) {
        this.umidadeCarga = umidadeCarga;
    }

    public Double getPesoCargaKg() {
        return pesoCargaKg;
    }

    public void setPesoCargaKg(Double pesoCargaKg) {
        this.pesoCargaKg = pesoCargaKg;
    }

    public Boolean getPortaBauAberta() {
        return portaBauAberta;
    }

    public void setPortaBauAberta(Boolean portaBauAberta) {
        this.portaBauAberta = portaBauAberta;
    }

    public Boolean getImpactoCarga() {
        return impactoCarga;
    }

    public void setImpactoCarga(Boolean impactoCarga) {
        this.impactoCarga = impactoCarga;
    }

    public Double getGForceImpacto() {
        return gForceImpacto;
    }

    public void setGForceImpacto(Double gForceImpacto) {
        this.gForceImpacto = gForceImpacto;
    }

    public Object getPressaoPneusJson() {
        return pressaoPneusJson;
    }

    public void setPressaoPneusJson(Object pressaoPneusJson) {
        this.pressaoPneusJson = pressaoPneusJson;
    }

    public Boolean getAlertaPneu() {
        return alertaPneu;
    }

    public void setAlertaPneu(Boolean alertaPneu) {
        this.alertaPneu = alertaPneu;
    }

    public Boolean getFadigaDetectada() {
        return fadigaDetectada;
    }

    public void setFadigaDetectada(Boolean fadigaDetectada) {
        this.fadigaDetectada = fadigaDetectada;
    }

    public Boolean getDistracaoDetectada() {
        return distracaoDetectada;
    }

    public void setDistracaoDetectada(Boolean distracaoDetectada) {
        this.distracaoDetectada = distracaoDetectada;
    }

    public Boolean getUsoCelularDetectado() {
        return usoCelularDetectado;
    }

    public void setUsoCelularDetectado(Boolean usoCelularDetectado) {
        this.usoCelularDetectado = usoCelularDetectado;
    }

    public Boolean getCigarroDetectado() {
        return cigarroDetectado;
    }

    public void setCigarroDetectado(Boolean cigarroDetectado) {
        this.cigarroDetectado = cigarroDetectado;
    }

    public Boolean getAusenciaCintoDms() {
        return ausenciaCintoDms;
    }

    public void setAusenciaCintoDms(Boolean ausenciaCintoDms) {
        this.ausenciaCintoDms = ausenciaCintoDms;
    }

    public Integer getScoreDms() {
        return scoreDms;
    }

    public void setScoreDms(Integer scoreDms) {
        this.scoreDms = scoreDms;
    }

    public Double getTemperaturaExterna() {
        return temperaturaExterna;
    }

    public void setTemperaturaExterna(Double temperaturaExterna) {
        this.temperaturaExterna = temperaturaExterna;
    }

    public Double getUmidadeExterna() {
        return umidadeExterna;
    }

    public void setUmidadeExterna(Double umidadeExterna) {
        this.umidadeExterna = umidadeExterna;
    }

    public Boolean getChuvaDetectada() {
        return chuvaDetectada;
    }

    public void setChuvaDetectada(Boolean chuvaDetectada) {
        this.chuvaDetectada = chuvaDetectada;
    }

    public String getCondicaoPista() {
        return condicaoPista;
    }

    public void setCondicaoPista(String condicaoPista) {
        this.condicaoPista = condicaoPista;
    }

    public Double getSinalGsm() {
        return sinalGsm;
    }

    public void setSinalGsm(Double sinalGsm) {
        this.sinalGsm = sinalGsm;
    }

    public Double getSinalGps() {
        return sinalGps;
    }

    public void setSinalGps(Double sinalGps) {
        this.sinalGps = sinalGps;
    }

    public String getTecnologiaRede() {
        return tecnologiaRede;
    }

    public void setTecnologiaRede(String tecnologiaRede) {
        this.tecnologiaRede = tecnologiaRede;
    }

    public String getFirmwareVersao() {
        return firmwareVersao;
    }

    public void setFirmwareVersao(String firmwareVersao) {
        this.firmwareVersao = firmwareVersao;
    }

    public Boolean getModoOffline() {
        return modoOffline;
    }

    public void setModoOffline(Boolean modoOffline) {
        this.modoOffline = modoOffline;
    }

    public Integer getDelaySincronizacaoS() {
        return delaySincronizacaoS;
    }

    public void setDelaySincronizacaoS(Integer delaySincronizacaoS) {
        this.delaySincronizacaoS = delaySincronizacaoS;
    }

    public String getTacografoStatus() {
        return tacografoStatus;
    }

    public void setTacografoStatus(String tacografoStatus) {
        this.tacografoStatus = tacografoStatus;
    }

    public Double getTacografoVelocidade() {
        return tacografoVelocidade;
    }

    public void setTacografoVelocidade(Double tacografoVelocidade) {
        this.tacografoVelocidade = tacografoVelocidade;
    }

    public Double getTacografoDistancia() {
        return tacografoDistancia;
    }

    public void setTacografoDistancia(Double tacografoDistancia) {
        this.tacografoDistancia = tacografoDistancia;
    }

    public Double getHorasDirecaoAcumuladas() {
        return horasDirecaoAcumuladas;
    }

    public void setHorasDirecaoAcumuladas(Double horasDirecaoAcumuladas) {
        this.horasDirecaoAcumuladas = horasDirecaoAcumuladas;
    }

    public Boolean getManutencaoPendente() {
        return manutencaoPendente;
    }

    public void setManutencaoPendente(Boolean manutencaoPendente) {
        this.manutencaoPendente = manutencaoPendente;
    }

    public LocalDateTime getProximaRevisao() {
        return proximaRevisao;
    }

    public void setProximaRevisao(LocalDateTime proximaRevisao) {
        this.proximaRevisao = proximaRevisao;
    }

    public Double getDesgasteFreio() {
        return desgasteFreio;
    }

    public void setDesgasteFreio(Double desgasteFreio) {
        this.desgasteFreio = desgasteFreio;
    }

    public Object getDtcCodes() {
        return dtcCodes;
    }

    public void setDtcCodes(Object dtcCodes) {
        this.dtcCodes = dtcCodes;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public LocalDateTime getRecebidoEm() {
        return recebidoEm;
    }

    public void setRecebidoEm(LocalDateTime recebidoEm) {
        this.recebidoEm = recebidoEm;
    }

    public LocalDateTime getProcessadoEm() {
        return processadoEm;
    }

    public void setProcessadoEm(LocalDateTime processadoEm) {
        this.processadoEm = processadoEm;
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
