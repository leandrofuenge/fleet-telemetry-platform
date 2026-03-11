package com.app.telemetria.entity;

import jakarta.persistence.*;
import lombok.*;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private Double kmExtras = 0.0;

    @Column(name = "alerta_enviado", nullable = false)
    @Builder.Default
    private Boolean alertaEnviado = false;

    @Column(name = "resolvido", nullable = false)
    @Builder.Default
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
}
