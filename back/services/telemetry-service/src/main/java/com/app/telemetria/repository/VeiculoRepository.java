package com.app.telemetria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.app.telemetria.entity.Veiculo;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
}
