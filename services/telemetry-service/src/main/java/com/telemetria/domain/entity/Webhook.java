package com.telemetria.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "webhooks", indexes = @Index(name = "idx_webhook_tenant", columnList = "tenant_id"))
public class Webhook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String evento;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "falhas_consecutivas", nullable = false)
    private Integer falhasConsecutivas = 0;
}
