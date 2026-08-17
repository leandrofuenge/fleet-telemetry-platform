package com.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.RntrcConsulta;

public interface RntrcConsultaRepository extends JpaRepository<RntrcConsulta, Long> {
    Optional<RntrcConsulta> findTopByRntrcOrderByDataConsultaDesc(String rntrc);
}
