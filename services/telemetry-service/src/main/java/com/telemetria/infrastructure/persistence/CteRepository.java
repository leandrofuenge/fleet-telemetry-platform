package com.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Cte;

public interface CteRepository extends JpaRepository<Cte, Long> {
    Optional<Cte> findByCargaId(Long cargaId);
}
