package com.telemetria.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.RegraAlerta;

public interface RegraAlertaRepository extends JpaRepository<RegraAlerta, Long> {
    List<RegraAlerta> findByTenantIdAndAtivoTrue(Long tenantId);
    List<RegraAlerta> findByTenantIdOrderByNomeAsc(Long tenantId);
}
