package com.app.telemetria.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "desvios_rota", indexes = {
        @Index(name = "idx_desvio_rota", columnList = "rota_id"),
        @Index(name = "idx_desvio_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_desvio_viagem", columnList = "viagem_id"),
        @Index(name = "idx_desvio_data", columnList = "data_hora_desvio"),
        @Index(name = "idx_desvio_resolvido", columnList = "resolvido")
})
public class DesvioRota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "rota_id", nullable = false)
    private Long rotaId;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(name = "veiculo_uuid", nullable = false, length = 36)
    private String veiculoUuid;

    @Column(name = "viagem_id")
    private Long viagemId;

    @Column(name = "latitude_desvio", nullable = false)
    private Double latitudeDesvio;

    @Column(name = "longitude_desvio", nullable = false)
    private Double longitudeDesvio;

    @Column(name = "velocidade_kmh")
    private Double velocidadeKmh;

    @Column(name = "distancia_metros", nullable = false)
    private Double distanciaMetros;

    @Column(name = "lat_ponto_mais_proximo")
    private Double latPontoMaisProximo;

    @Column(name = "lng_ponto_mais_proximo")
    private Double lngPontoMaisProximo;

    @Column(name = "nome_via_desvio")
    private String nomeViaDesvio;

    @Column(name = "data_hora_desvio", nullable = false)
    private LocalDateTime dataHoraDesvio;

    @Column(name = "data_hora_retorno")
    private LocalDateTime dataHoraRetorno;

    @Column(name = "duracao_min")
    private Integer duracaoMin;

    @Column(name = "km_extras", nullable = false)
    private Double kmExtras = 0.0;

    @Column(name = "alerta_enviado", nullable = false)
    private Boolean alertaEnviado = false;

    @Column(name = "resolvido", nullable = false)
    private Boolean resolvido = false;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    // ── Relacionamentos JPA (objetos completos) ────────────────

    /**
     * Veículo que realizou o desvio.
     * FK: veiculo_id → veiculos.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", insertable = false, updatable = false)
    private Veiculo veiculo;

    /**
     * Rota da qual ocorreu o desvio.
     * FK: rota_id → rotas.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rota_id", insertable = false, updatable = false)
    private Rota rota;

    /**
     * Viagem em execução no momento do desvio.
     * FK: viagem_id → viagens.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;

    // ================================
    // Construtores
    // ================================

    public DesvioRota() {
    }

    public DesvioRota(Long id, Long tenantId, Long rotaId, Long veiculoId, String veiculoUuid,
                      Long viagemId, Double latitudeDesvio, Double longitudeDesvio,
                      Double velocidadeKmh, Double distanciaMetros, Double latPontoMaisProximo,
                      Double lngPontoMaisProximo, String nomeViaDesvio, LocalDateTime dataHoraDesvio,
                      LocalDateTime dataHoraRetorno, Integer duracaoMin, Double kmExtras,
                      Boolean alertaEnviado, Boolean resolvido, String motivo, LocalDateTime criadoEm,
                      Veiculo veiculo, Rota rota, Viagem viagem) {
        this.id = id;
        this.tenantId = tenantId;
        this.rotaId = rotaId;
        this.veiculoId = veiculoId;
        this.veiculoUuid = veiculoUuid;
        this.viagemId = viagemId;
        this.latitudeDesvio = latitudeDesvio;
        this.longitudeDesvio = longitudeDesvio;
        this.velocidadeKmh = velocidadeKmh;
        this.distanciaMetros = distanciaMetros;
        this.latPontoMaisProximo = latPontoMaisProximo;
        this.lngPontoMaisProximo = lngPontoMaisProximo;
        this.nomeViaDesvio = nomeViaDesvio;
        this.dataHoraDesvio = dataHoraDesvio;
        this.dataHoraRetorno = dataHoraRetorno;
        this.duracaoMin = duracaoMin;
        this.kmExtras = kmExtras != null ? kmExtras : 0.0;
        this.alertaEnviado = alertaEnviado != null ? alertaEnviado : false;
        this.resolvido = resolvido != null ? resolvido : false;
        this.motivo = motivo;
        this.criadoEm = criadoEm;
        this.veiculo = veiculo;
        this.rota = rota;
        this.viagem = viagem;
    }

    // ================================
    // Getters e Setters
    // ================================

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

    public Long getViagemId() {
        return viagemId;
    }

    public void setViagemId(Long viagemId) {
        this.viagemId = viagemId;
    }

    public Double getLatitudeDesvio() {
        return latitudeDesvio;
    }

    public void setLatitudeDesvio(Double latitudeDesvio) {
        this.latitudeDesvio = latitudeDesvio;
    }

    public Double getLongitudeDesvio() {
        return longitudeDesvio;
    }

    public void setLongitudeDesvio(Double longitudeDesvio) {
        this.longitudeDesvio = longitudeDesvio;
    }

    public Double getVelocidadeKmh() {
        return velocidadeKmh;
    }

    public void setVelocidadeKmh(Double velocidadeKmh) {
        this.velocidadeKmh = velocidadeKmh;
    }

    public Double getDistanciaMetros() {
        return distanciaMetros;
    }

    public void setDistanciaMetros(Double distanciaMetros) {
        this.distanciaMetros = distanciaMetros;
    }

    public Double getLatPontoMaisProximo() {
        return latPontoMaisProximo;
    }

    public void setLatPontoMaisProximo(Double latPontoMaisProximo) {
        this.latPontoMaisProximo = latPontoMaisProximo;
    }

    public Double getLngPontoMaisProximo() {
        return lngPontoMaisProximo;
    }

    public void setLngPontoMaisProximo(Double lngPontoMaisProximo) {
        this.lngPontoMaisProximo = lngPontoMaisProximo;
    }

    public String getNomeViaDesvio() {
        return nomeViaDesvio;
    }

    public void setNomeViaDesvio(String nomeViaDesvio) {
        this.nomeViaDesvio = nomeViaDesvio;
    }

    public LocalDateTime getDataHoraDesvio() {
        return dataHoraDesvio;
    }

    public void setDataHoraDesvio(LocalDateTime dataHoraDesvio) {
        this.dataHoraDesvio = dataHoraDesvio;
    }

    public LocalDateTime getDataHoraRetorno() {
        return dataHoraRetorno;
    }

    public void setDataHoraRetorno(LocalDateTime dataHoraRetorno) {
        this.dataHoraRetorno = dataHoraRetorno;
    }

    public Integer getDuracaoMin() {
        return duracaoMin;
    }

    public void setDuracaoMin(Integer duracaoMin) {
        this.duracaoMin = duracaoMin;
    }

    public Double getKmExtras() {
        return kmExtras;
    }

    public void setKmExtras(Double kmExtras) {
        this.kmExtras = kmExtras != null ? kmExtras : 0.0;
    }

    public Boolean getAlertaEnviado() {
        return alertaEnviado;
    }

    public void setAlertaEnviado(Boolean alertaEnviado) {
        this.alertaEnviado = alertaEnviado != null ? alertaEnviado : false;
    }

    public Boolean getResolvido() {
        return resolvido;
    }

    public void setResolvido(Boolean resolvido) {
        this.resolvido = resolvido != null ? resolvido : false;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
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

    public Rota getRota() {
        return rota;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
    }

    public Viagem getViagem() {
        return viagem;
    }

    public void setViagem(Viagem viagem) {
        this.viagem = viagem;
    }

    // ================================
    // Método Builder manual
    // ================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long rotaId;
        private Long veiculoId;
        private String veiculoUuid;
        private Long viagemId;
        private Double latitudeDesvio;
        private Double longitudeDesvio;
        private Double velocidadeKmh;
        private Double distanciaMetros;
        private Double latPontoMaisProximo;
        private Double lngPontoMaisProximo;
        private String nomeViaDesvio;
        private LocalDateTime dataHoraDesvio;
        private LocalDateTime dataHoraRetorno;
        private Integer duracaoMin;
        private Double kmExtras = 0.0;
        private Boolean alertaEnviado = false;
        private Boolean resolvido = false;
        private String motivo;
        private LocalDateTime criadoEm;
        private Veiculo veiculo;
        private Rota rota;
        private Viagem viagem;

        Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
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

        public Builder viagemId(Long viagemId) {
            this.viagemId = viagemId;
            return this;
        }

        public Builder latitudeDesvio(Double latitudeDesvio) {
            this.latitudeDesvio = latitudeDesvio;
            return this;
        }

        public Builder longitudeDesvio(Double longitudeDesvio) {
            this.longitudeDesvio = longitudeDesvio;
            return this;
        }

        public Builder velocidadeKmh(Double velocidadeKmh) {
            this.velocidadeKmh = velocidadeKmh;
            return this;
        }

        public Builder distanciaMetros(Double distanciaMetros) {
            this.distanciaMetros = distanciaMetros;
            return this;
        }

        public Builder latPontoMaisProximo(Double latPontoMaisProximo) {
            this.latPontoMaisProximo = latPontoMaisProximo;
            return this;
        }

        public Builder lngPontoMaisProximo(Double lngPontoMaisProximo) {
            this.lngPontoMaisProximo = lngPontoMaisProximo;
            return this;
        }

        public Builder nomeViaDesvio(String nomeViaDesvio) {
            this.nomeViaDesvio = nomeViaDesvio;
            return this;
        }

        public Builder dataHoraDesvio(LocalDateTime dataHoraDesvio) {
            this.dataHoraDesvio = dataHoraDesvio;
            return this;
        }

        public Builder dataHoraRetorno(LocalDateTime dataHoraRetorno) {
            this.dataHoraRetorno = dataHoraRetorno;
            return this;
        }

        public Builder duracaoMin(Integer duracaoMin) {
            this.duracaoMin = duracaoMin;
            return this;
        }

        public Builder kmExtras(Double kmExtras) {
            this.kmExtras = kmExtras;
            return this;
        }

        public Builder alertaEnviado(Boolean alertaEnviado) {
            this.alertaEnviado = alertaEnviado;
            return this;
        }

        public Builder resolvido(Boolean resolvido) {
            this.resolvido = resolvido;
            return this;
        }

        public Builder motivo(String motivo) {
            this.motivo = motivo;
            return this;
        }

        public Builder criadoEm(LocalDateTime criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Builder veiculo(Veiculo veiculo) {
            this.veiculo = veiculo;
            return this;
        }

        public Builder rota(Rota rota) {
            this.rota = rota;
            return this;
        }

        public Builder viagem(Viagem viagem) {
            this.viagem = viagem;
            return this;
        }

        public DesvioRota build() {
            return new DesvioRota(
                this.id, this.tenantId, this.rotaId, this.veiculoId, this.veiculoUuid,
                this.viagemId, this.latitudeDesvio, this.longitudeDesvio, this.velocidadeKmh,
                this.distanciaMetros, this.latPontoMaisProximo, this.lngPontoMaisProximo,
                this.nomeViaDesvio, this.dataHoraDesvio, this.dataHoraRetorno, this.duracaoMin,
                this.kmExtras, this.alertaEnviado, this.resolvido, this.motivo, this.criadoEm,
                this.veiculo, this.rota, this.viagem
            );
        }
    }
}