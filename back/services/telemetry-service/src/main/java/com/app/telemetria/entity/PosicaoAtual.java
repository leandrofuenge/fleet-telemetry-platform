package com.app.telemetria.entity;

// ─────────────────────────────────────────────────────────────
// 8. PosicaoAtual.java
// ─────────────────────────────────────────────────────────────

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "posicao_atual", indexes = {
        @Index(name = "idx_pa_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pa_status", columnList = "status_veiculo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosicaoAtual {

    @Id
    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "veiculo_uuid", nullable = false, length = 36)
    private String veiculoUuid;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "velocidade", nullable = false)
    @Builder.Default
    private Double velocidade = 0.0;

    @Column(name = "direcao")
    private Double direcao;

    @Column(name = "ignicao", nullable = false)
    @Builder.Default
    private Boolean ignicao = false;

    @Column(name = "status_veiculo", nullable = false, length = 30)
    @Builder.Default
    private String statusVeiculo = "DESCONHECIDO";

    @Column(name = "motorista_id")
    private Long motoristaId;

    @Column(name = "viagem_id")
    private Long viagemId;

    @Column(name = "odometro")
    private Double odometro;

    @Column(name = "nivel_combustivel")
    private Double nivelCombustivel;

    @Column(name = "bateria_v")
    private Double bateriaV;

    @Column(name = "ultima_telemetria", nullable = false)
    private LocalDateTime ultimaTelemetria;

    @UpdateTimestamp
    @Column(name = "ultima_atualizacao")
    private LocalDateTime ultimaAtualizacao;

    @Column(name = "nome_local")
    private String nomeLocal;

    @Column(name = "alertas_ativos", nullable = false)
    @Builder.Default
    private Integer alertasAtivos = 0;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private VeiculoCache veiculo;
}
