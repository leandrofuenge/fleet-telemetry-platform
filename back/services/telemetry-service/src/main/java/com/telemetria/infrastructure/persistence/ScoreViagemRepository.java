package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.ScoreViagem;

@Repository
public interface ScoreViagemRepository extends JpaRepository<ScoreViagem, Long> {

    /**
     * Busca score por viagem
     */
    Optional<ScoreViagem> findByViagemId(Long viagemId);

    /**
     * Lista scores por motorista (ordenados do mais recente)
     */
    List<ScoreViagem> findByMotoristaIdOrderByDataCalculoDesc(Long motoristaId);

    /**
     * Lista scores por veículo
     */
    List<ScoreViagem> findByVeiculoIdOrderByDataCalculoDesc(Long veiculoId);

    /**
     * Busca scores críticos (< 700) não notificados
     */
    @Query("SELECT s FROM ScoreViagem s WHERE s.scoreFinal < 700 AND s.notificacaoGestorEnviada = false")
    List<ScoreViagem> findScoresCriticosNaoNotificados();

    /**
     * Busca scores por período
     */
    List<ScoreViagem> findByDataCalculoBetween(LocalDateTime inicio, LocalDateTime fim);

    /**
     * Calcula média de score por motorista
     */
    @Query("SELECT AVG(s.scoreFinal) FROM ScoreViagem s WHERE s.motoristaId = :motoristaId")
    Double calcularMediaScoreMotorista(@Param("motoristaId") Long motoristaId);

    /**
     * Busca scores paginados por motorista
     */
    Page<ScoreViagem> findByMotoristaId(Long motoristaId, Pageable pageable);
}