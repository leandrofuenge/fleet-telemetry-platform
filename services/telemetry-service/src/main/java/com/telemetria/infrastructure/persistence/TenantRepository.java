package com.telemetria.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Tenant;
import com.telemetria.domain.enums.StatusTenant;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
    List<Tenant> findByStatus(StatusTenant status);
}
