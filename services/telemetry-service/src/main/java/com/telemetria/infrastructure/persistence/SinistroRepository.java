package com.telemetria.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Sinistro;

public interface SinistroRepository extends JpaRepository<Sinistro, Long> {
}
