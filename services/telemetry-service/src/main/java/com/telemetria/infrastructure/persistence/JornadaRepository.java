package com.telemetria.infrastructure.persistence;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Jornada;
import com.telemetria.domain.enums.StatusJornada;
public interface JornadaRepository extends JpaRepository<Jornada,Long> {
 Optional<Jornada> findTopByMotoristaIdAndStatusOrderByDataInicioDesc(Long motoristaId, StatusJornada status);
 Optional<Jornada> findTopByMotoristaIdOrderByDataFimDesc(Long motoristaId);
}
