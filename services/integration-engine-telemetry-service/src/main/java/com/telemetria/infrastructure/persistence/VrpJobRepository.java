package com.telemetria.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.VrpJob;

public interface VrpJobRepository extends JpaRepository<VrpJob, Long> {
}
