package com.telemetria.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.enums.TipoDispositivo;

@Repository
public interface DispositivoIotRepository extends JpaRepository<DispositivoIot, Long> {

    Optional<DispositivoIot> findByDeviceId(String deviceId);
    
    List<DispositivoIot> findByVeiculoId(Long veiculoId);
    
    long countByVeiculoId(Long veiculoId);
    
    @Query("SELECT d FROM DispositivoIot d WHERE d.veiculoId = :veiculoId AND d.tipo = :tipo")
    Optional<DispositivoIot> findByVeiculoIdAndTipo(Long veiculoId, TipoDispositivo tipo);
    
    boolean existsByDeviceIdAndVeiculoIdNot(String deviceId, Long veiculoId);
}