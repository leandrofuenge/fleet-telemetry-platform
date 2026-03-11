package com.app.telemetria.entity;

// ─────────────────────────────────────────────────────────────
// 2. MotoristaCache.java
// ─────────────────────────────────────────────────────────────

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "motoristas_cache", indexes = {
        @Index(name = "idx_mc_tenant", columnList = "tenant_id"),
        @Index(name = "idx_mc_cpf", columnList = "cpf")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotoristaCache {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "cpf", nullable = false, length = 14)
    private String cpf;

    @Column(name = "cnh", nullable = false, length = 50)
    private String cnh;

    @Column(name = "categoria_cnh", nullable = false, length = 10)
    private String categoriaCnh;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @UpdateTimestamp
    @Column(name = "sincronizado_em")
    private LocalDateTime sincronizadoEm;
}
