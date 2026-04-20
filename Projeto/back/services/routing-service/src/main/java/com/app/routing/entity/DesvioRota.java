package com.app.routing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "desvios_rota", indexes = {
        @Index(name = "idx_dr_viagem", columnList = "viagem_id"),
        @Index(name = "idx_dr_rota", columnList = "rota_id"),
        @Index(name = "idx_dr_data", columnList = "data_hora_desvio"),
        @Index(name = "idx_dr_resolvido", columnList = "resolvido")
})
public class DesvioRota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "viagem_id", nullable = false)
    private Long viagemId;

    @Column(name = "rota_id", nullable = false)
    private Long rotaId;

    @Column(name = "veiculo_uuid", nullable = false, length = 36)
    private String veiculoUuid;

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

    @Column(name = "nome_via_rota")
    private String nomeViaRota;

    @Column(name = "data_hora_desvio", nullable = false)
    private LocalDateTime dataHoraDesvio;

    @Column(name = "data_hora_retorno")
    private LocalDateTime dataHoraRetorno;

    @Column(name = "duracao_min")
    private Integer duracaoMin;

    @Column(name = "km_extras", nullable = false)
    private Double kmExtras;

    @Column(name = "alerta_enviado", nullable = false)
    private Boolean alertaEnviado;

    @Column(name = "alerta_id")
    private Long alertaId;

    @Column(name = "resolvido", nullable = false)
    private Boolean resolvido;

    @Column(name = "motivo_motorista", length = 255)
    private String motivoMotorista;

    @Column(name = "aprovado_gestor")
    private Boolean aprovadoGestor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rota_id", insertable = false, updatable = false)
    private Rota rota;

    // ==================== CONSTRUTORES ====================

    /**
     * Construtor padrão (sem argumentos).
     * Inicializa os campos com os valores padrão definidos nos @Builder.Default
     * originais.
     */
    public DesvioRota() {
        this.kmExtras = 0.0;
        this.alertaEnviado = false;
        this.resolvido = false;
    }

    /**
     * Construtor privado com todos os campos.
     * Usado internamente pelo Builder.
     */
    private DesvioRota(Long id, Long tenantId, Long viagemId, Long rotaId, String veiculoUuid,
            Double latitudeDesvio, Double longitudeDesvio, Double velocidadeKmh,
            Double distanciaMetros, Double latPontoMaisProximo, Double lngPontoMaisProximo,
            String nomeViaDesvio, String nomeViaRota, LocalDateTime dataHoraDesvio,
            LocalDateTime dataHoraRetorno, Integer duracaoMin, Double kmExtras,
            Boolean alertaEnviado, Long alertaId, Boolean resolvido, String motivoMotorista,
            Boolean aprovadoGestor, LocalDateTime criadoEm, Viagem viagem, Rota rota) {
        this.id = id;
        this.tenantId = tenantId;
        this.viagemId = viagemId;
        this.rotaId = rotaId;
        this.veiculoUuid = veiculoUuid;
        this.latitudeDesvio = latitudeDesvio;
        this.longitudeDesvio = longitudeDesvio;
        this.velocidadeKmh = velocidadeKmh;
        this.distanciaMetros = distanciaMetros;
        this.latPontoMaisProximo = latPontoMaisProximo;
        this.lngPontoMaisProximo = lngPontoMaisProximo;
        this.nomeViaDesvio = nomeViaDesvio;
        this.nomeViaRota = nomeViaRota;
        this.dataHoraDesvio = dataHoraDesvio;
        this.dataHoraRetorno = dataHoraRetorno;
        this.duracaoMin = duracaoMin;
        this.kmExtras = kmExtras != null ? kmExtras : 0.0;
        this.alertaEnviado = alertaEnviado != null ? alertaEnviado : false;
        this.alertaId = alertaId;
        this.resolvido = resolvido != null ? resolvido : false;
        this.motivoMotorista = motivoMotorista;
        this.aprovadoGestor = aprovadoGestor;
        this.criadoEm = criadoEm;
        this.viagem = viagem;
        this.rota = rota;
    }

    // ==================== GETTERS E SETTERS ====================

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

    public Long getViagemId() {
        return viagemId;
    }

    public void setViagemId(Long viagemId) {
        this.viagemId = viagemId;
    }

    public Long getRotaId() {
        return rotaId;
    }

    public void setRotaId(Long rotaId) {
        this.rotaId = rotaId;
    }

    public String getVeiculoUuid() {
        return veiculoUuid;
    }

    public void setVeiculoUuid(String veiculoUuid) {
        this.veiculoUuid = veiculoUuid;
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

    public String getNomeViaRota() {
        return nomeViaRota;
    }

    public void setNomeViaRota(String nomeViaRota) {
        this.nomeViaRota = nomeViaRota;
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
        this.kmExtras = kmExtras;
    }

    public Boolean getAlertaEnviado() {
        return alertaEnviado;
    }

    public void setAlertaEnviado(Boolean alertaEnviado) {
        this.alertaEnviado = alertaEnviado;
    }

    public Long getAlertaId() {
        return alertaId;
    }

    public void setAlertaId(Long alertaId) {
        this.alertaId = alertaId;
    }

    public Boolean getResolvido() {
        return resolvido;
    }

    public void setResolvido(Boolean resolvido) {
        this.resolvido = resolvido;
    }

    public String getMotivoMotorista() {
        return motivoMotorista;
    }

    public void setMotivoMotorista(String motivoMotorista) {
        this.motivoMotorista = motivoMotorista;
    }

    public Boolean getAprovadoGestor() {
        return aprovadoGestor;
    }

    public void setAprovadoGestor(Boolean aprovadoGestor) {
        this.aprovadoGestor = aprovadoGestor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    // Não criar setter para criadoEm porque é @CreationTimestamp e updatable=false
    // (pode ser deixado, mas geralmente não se modifica manualmente)
    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Viagem getViagem() {
        return viagem;
    }

    public void setViagem(Viagem viagem) {
        this.viagem = viagem;
    }

    public Rota getRota() {
        return rota;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private Long tenantId;
        private Long viagemId;
        private Long rotaId;
        private String veiculoUuid;
        private Double latitudeDesvio;
        private Double longitudeDesvio;
        private Double velocidadeKmh;
        private Double distanciaMetros;
        private Double latPontoMaisProximo;
        private Double lngPontoMaisProximo;
        private String nomeViaDesvio;
        private String nomeViaRota;
        private LocalDateTime dataHoraDesvio;
        private LocalDateTime dataHoraRetorno;
        private Integer duracaoMin;
        private Double kmExtras = 0.0; // valor padrão
        private Boolean alertaEnviado = false; // valor padrão
        private Long alertaId;
        private Boolean resolvido = false; // valor padrão
        private String motivoMotorista;
        private Boolean aprovadoGestor;
        private LocalDateTime criadoEm;
        private Viagem viagem;
        private Rota rota;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder viagemId(Long viagemId) {
            this.viagemId = viagemId;
            return this;
        }

        public Builder rotaId(Long rotaId) {
            this.rotaId = rotaId;
            return this;
        }

        public Builder veiculoUuid(String veiculoUuid) {
            this.veiculoUuid = veiculoUuid;
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

        public Builder nomeViaRota(String nomeViaRota) {
            this.nomeViaRota = nomeViaRota;
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

        public Builder alertaId(Long alertaId) {
            this.alertaId = alertaId;
            return this;
        }

        public Builder resolvido(Boolean resolvido) {
            this.resolvido = resolvido;
            return this;
        }

        public Builder motivoMotorista(String motivoMotorista) {
            this.motivoMotorista = motivoMotorista;
            return this;
        }

        public Builder aprovadoGestor(Boolean aprovadoGestor) {
            this.aprovadoGestor = aprovadoGestor;
            return this;
        }

        public Builder criadoEm(LocalDateTime criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Builder viagem(Viagem viagem) {
            this.viagem = viagem;
            return this;
        }

        public Builder rota(Rota rota) {
            this.rota = rota;
            return this;
        }

        public DesvioRota build() {
            return new DesvioRota(
                    id, tenantId, viagemId, rotaId, veiculoUuid,
                    latitudeDesvio, longitudeDesvio, velocidadeKmh,
                    distanciaMetros, latPontoMaisProximo, lngPontoMaisProximo,
                    nomeViaDesvio, nomeViaRota, dataHoraDesvio,
                    dataHoraRetorno, duracaoMin, kmExtras,
                    alertaEnviado, alertaId, resolvido, motivoMotorista,
                    aprovadoGestor, criadoEm, viagem, rota);
        }
    }
}