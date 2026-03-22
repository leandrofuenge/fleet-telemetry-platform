package com.app.telemetria.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.HistoricoOdometro;

@Repository
public interface HistoricoOdometroRepository extends JpaRepository<HistoricoOdometro, Long> {

    List<HistoricoOdometro> findByVeiculoIdOrderByDataTrocaDesc(Long veiculoId);
    
    Optional<HistoricoOdometro> findTopByVeiculoIdOrderByDataTrocaDesc(Long veiculoId);
    
    @Query("SELECT h FROM HistoricoOdometro h WHERE h.veiculoId = :veiculoId AND h.alertaInconsistencia = true")
    List<HistoricoOdometro> findInconsistenciasByVeiculoId(@Param("veiculoId") Long veiculoId);
}