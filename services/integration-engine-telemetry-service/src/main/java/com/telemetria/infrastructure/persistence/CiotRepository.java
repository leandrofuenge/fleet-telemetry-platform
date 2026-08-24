package com.telemetria.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Ciot;

public interface CiotRepository extends JpaRepository<Ciot, Long> {
    boolean existsByViagemId(Long viagemId);
}
