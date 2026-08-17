package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.telemetria.domain.enums.OrigemDado;
import com.telemetria.domain.enums.StatusJornada;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "jornadas", indexes = {
        @Index(name = "idx_jornada_motorista", columnList = "motorista_id"),
        @Index(name = "idx_jornada_tenant", columnList = "tenant_id"),
        @Index(name = "idx_jornada_data", columnList = "data_inicio"),
        @Index(name = "idx_jornada_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "motorista_id", nullable = false)
    private Long motoristaId;

    @Column(name = "veiculo_id")
    private Long veiculoId;

    @Column(name = "viagem_id")
    private Long viagemId;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Column(name = "horas_direcao", nullable = false)
    @Builder.Default
    private Double horasDirecao = 0.0;
    @Column(name = "horas_disponivel", nullable = false)
    @Builder.Default
    private Double horasDisponivel = 0.0;
    @Column(name = "horas_repouso", nullable = false)
    @Builder.Default
    private Double horasRepouso = 0.0;
    @Column(name = "horas_extras", nullable = false)
    @Builder.Default
    private Double horasExtras = 0.0;
    @Column(name = "pausas_realizadas", nullable = false)
    @Builder.Default
    private Integer pausasRealizadas = 0;
    @Column(name = "km_rodados", nullable = false)
    @Builder.Default
    private Double kmRodados = 0.0;

    @Column(name = "limite_direcao_h", nullable = false)
    @Builder.Default
    private Double limiteDirecaoH = 8.0;

    @Column(name = "limite_extra_h", nullable = false)
    @Builder.Default
    private Double limiteExtraH = 2.0;

    @Column(name = "alertas_enviados", nullable = false)
    @Builder.Default
    private Integer alertasEnviados = 0;

    @Column(name = "alerta_limite_30min", nullable = false)
    @Builder.Default
    private Boolean alertaLimite30min = false;

    @Column(name = "status", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusJornada status = StatusJornada.ABERTA;

    @Column(name = "origem_dado", nullable = false, length = 15)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrigemDado origemDado = OrigemDado.TELEMETRIA;

    @Column(name = "arquivo_tacografo", length = 200)
    private String arquivoTacografo;

    @Column(name = "irregular", nullable = false)
    @Builder.Default
    private Boolean irregular = false;

    @Column(name = "motivo_irregularidade", columnDefinition = "TEXT")
    private String motivoIrregularidade;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Relacionamentos JPA (objetos completos) ────────────────

    /**
     * Motorista ao qual pertence esta jornada.
     * FK: motorista_id → motoristas.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", insertable = false, updatable = false)
    private Motorista motorista;

    /**
     * Veículo utilizado nesta jornada.
     * FK: veiculo_id → veiculos.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", insertable = false, updatable = false)
    private Veiculo veiculo;

    /**
     * Viagem vinculada a esta jornada (opcional).
     * FK: viagem_id → viagens.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id", insertable = false, updatable = false)
    private Viagem viagem;
    public LocalDateTime getDataFim() { return dataFim; }
    public void setDataFim(LocalDateTime value) { dataFim = value; }
    public Double getHorasDirecao() { return horasDirecao; }
    public void setHorasDirecao(Double value) { horasDirecao = value; }
    public Double getHorasDisponivel() { return horasDisponivel; }
    public void setHorasDisponivel(Double value) { horasDisponivel = value; }
    public Boolean getIrregular() { return irregular; }
    public void setIrregular(Boolean value) { irregular = value; }
    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long value) { motoristaId = value; }
    public void setStatus(StatusJornada value) { status = value; }
    public void setAlertaLimite30min(Boolean value) { alertaLimite30min = value; }
    public void setMotivoIrregularidade(String value) { motivoIrregularidade = value; }

    public static JornadaBuilder builder() { return new JornadaBuilder(); }

    public static class JornadaBuilder {
        private Long tenantId;
        private Long motoristaId;
        private Long veiculoId;
        private Long viagemId;
        private LocalDateTime dataInicio;
        private OrigemDado origemDado;

        public JornadaBuilder tenantId(Long value) { tenantId = value; return this; }
        public JornadaBuilder motoristaId(Long value) { motoristaId = value; return this; }
        public JornadaBuilder veiculoId(Long value) { veiculoId = value; return this; }
        public JornadaBuilder viagemId(Long value) { viagemId = value; return this; }
        public JornadaBuilder dataInicio(LocalDateTime value) { dataInicio = value; return this; }
        public JornadaBuilder origemDado(OrigemDado value) { origemDado = value; return this; }
        public Jornada build() {
            Jornada jornada = new Jornada();
            jornada.tenantId = tenantId;
            jornada.motoristaId = motoristaId;
            jornada.veiculoId = veiculoId;
            jornada.viagemId = viagemId;
            jornada.dataInicio = dataInicio;
            if (origemDado != null) jornada.origemDado = origemDado;
            return jornada;
        }
    }
}
