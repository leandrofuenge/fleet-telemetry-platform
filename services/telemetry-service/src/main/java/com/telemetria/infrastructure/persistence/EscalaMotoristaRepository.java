package com.telemetria.infrastructure.persistence;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.telemetria.domain.entity.EscalaMotorista;
public interface EscalaMotoristaRepository extends JpaRepository<EscalaMotorista,Long> {
 @Query("select e from EscalaMotorista e where e.motoristaId=:motoristaId and e.status <> com.telemetria.domain.enums.StatusEscala.CANCELADA and e.dataInicioTurno < :fim and e.dataFimTurno > :inicio")
 List<EscalaMotorista> conflitosMotorista(@Param("motoristaId") Long motoristaId,@Param("inicio") LocalDateTime inicio,@Param("fim") LocalDateTime fim);
 List<EscalaMotorista> findByDataInicioTurnoBetween(LocalDateTime inicio, LocalDateTime fim);
}
