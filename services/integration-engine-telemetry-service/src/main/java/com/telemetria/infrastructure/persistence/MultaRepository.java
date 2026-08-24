package com.telemetria.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Multa;

public interface MultaRepository extends JpaRepository<Multa, Long> {
}
