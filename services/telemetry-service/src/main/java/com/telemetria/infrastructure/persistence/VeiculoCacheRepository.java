package com.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.VeiculoCache;

@Repository
public interface VeiculoCacheRepository extends JpaRepository<VeiculoCache, Long> {
    
    Optional<VeiculoCache> findByDeviceId(String deviceId);
    
    boolean existsById(Long id);
}
