package com.telemetria.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.OcorrenciaOperacional;

public interface OcorrenciaOperacionalRepository extends JpaRepository<OcorrenciaOperacional, Long> {
}
