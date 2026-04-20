package com.telemetria.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.Geofence;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    List<Geofence> findByAtivoTrueAndTenantId(Long tenantId);

    @Query(value = "SELECT * FROM geofences g WHERE g.ativo = 1 AND g.tenant_id = :tenantId AND (g.aplica_todos = 1 OR JSON_CONTAINS(g.veiculos_uuid, JSON_QUOTE(:veiculoUuid)))", nativeQuery = true)
    List<Geofence> findAtivasPorVeiculo(@Param("tenantId") Long tenantId, @Param("veiculoUuid") String veiculoUuid);
}