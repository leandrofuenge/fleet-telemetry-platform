package com.app.telemetria.entity;

// ─────────────────────────────────────────────────────────────
// 9. ResumoDiarioVeiculo.java
// ─────────────────────────────────────────────────────────────
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumo_diario_veiculo", uniqueConstraints = @UniqueConstraint(name = "uk_resumo_veiculo_data", columnNames = {
                "tenant_id", "veiculo_id", "data" }), indexes = {
                                @Index(name = "idx_rdv_tenant", columnList = "tenant_id"),
                                @Index(name = "idx_rdv_veiculo", columnList = "veiculo_id"),
                                @Index(name = "idx_rdv_data", columnList = "data")
                })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumoDiarioVeiculo {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "tenant_id", nullable = false)
        private Long tenantId;

        @Column(name = "veiculo_id", nullable = false)
        private Long veiculoId;

        @Column(name = "data", nullable = false)
        private LocalDate data;

        @Column(name = "km_total", nullable = false)
        @Builder.Default
        private Double kmTotal = 0.0;
        @Column(name = "horas_uso", nullable = false)
        @Builder.Default
        private Double horasUso = 0.0;
        @Column(name = "horas_ocioso", nullable = false)
        @Builder.Default
        private Double horasOcioso = 0.0;
        @Column(name = "litros_consumidos", nullable = false)
        @Builder.Default
        private Double litrosConsumidos = 0.0;
        @Column(name = "consumo_medio")
        private Double consumoMedio;
        @Column(name = "velocidade_media")
        private Double velocidadeMedia;
        @Column(name = "velocidade_maxima")
        private Double velocidadeMaxima;
        @Column(name = "frenagens_bruscas", nullable = false)
        @Builder.Default
        private Integer frenagensBruscas = 0;
        @Column(name = "aceleracoes_bruscas", nullable = false)
        @Builder.Default
        private Integer aceleracoesBruscas = 0;
        @Column(name = "excessos_velocidade", nullable = false)
        @Builder.Default
        private Integer excessosVelocidade = 0;
        @Column(name = "curvas_bruscas", nullable = false)
        @Builder.Default
        private Integer curvasBruscas = 0;
        @Column(name = "total_alertas", nullable = false)
        @Builder.Default
        private Integer totalAlertas = 0;
        @Column(name = "alertas_criticos", nullable = false)
        @Builder.Default
        private Integer alertasCriticos = 0;
        @Column(name = "total_viagens", nullable = false)
        @Builder.Default
        private Integer totalViagens = 0;
        @Column(name = "alertas_fadiga", nullable = false)
        @Builder.Default
        private Integer alertasFadiga = 0;
        @Column(name = "alertas_celular", nullable = false)
        @Builder.Default
        private Integer alertasCelular = 0;
        @Column(name = "score_dia", nullable = false)
        @Builder.Default
        private Integer scoreDia = 1000;
        @Column(name = "total_eventos", nullable = false)
        @Builder.Default
        private Integer totalEventos = 0;

        @CreationTimestamp
        @Column(name = "criado_em", nullable = false, updatable = false)
        private LocalDateTime criadoEm;
}
