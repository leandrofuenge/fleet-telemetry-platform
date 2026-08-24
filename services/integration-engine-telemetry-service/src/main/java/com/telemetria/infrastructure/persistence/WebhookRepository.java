package com.telemetria.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Webhook;

public interface WebhookRepository extends JpaRepository<Webhook, Long> {
    List<Webhook> findByTenantIdAndAtivoTrue(Long tenantId);
}
