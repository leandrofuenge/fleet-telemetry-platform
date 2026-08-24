package com.telemetria.infrastructure.persistence; import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.OtaJob;
public interface OtaJobRepository extends JpaRepository<OtaJob,Long>{List<OtaJob> findByStatus(String status);long countByStatus(String status);}
