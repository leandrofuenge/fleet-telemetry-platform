package com.app.telemetria.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.HistoricoScoreMotorista;

@Repository
public interface HistoricoScoreMotoristaRepository extends JpaRepository<HistoricoScoreMotorista, Long> {

    List<HistoricoScoreMotorista> findByMotoristaIdOrderByDataDesc(Long motoristaId);
    
    Optional<HistoricoScoreMotorista> findTopByMotoristaIdOrderByDataDesc(Long motoristaId);
    
    @Query("SELECT h FROM HistoricoScoreMotorista h WHERE h.motoristaId = :motoristaId AND h.data >= :dataInicio")
    List<HistoricoScoreMotorista> findByMotoristaIdAndDataAfter(@Param("motoristaId") Long motoristaId, 
                                                                 @Param("dataInicio") LocalDate dataInicio);
}