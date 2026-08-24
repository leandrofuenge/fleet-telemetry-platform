package com.telemetria.infrastructure.persistence; import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Manutencao;
public interface ManutencaoRepository extends JpaRepository<Manutencao,Long>{ List<Manutencao> findByVeiculoIdAndStatus(Long veiculoId,String status); List<Manutencao> findByStatusAndDataAgendadaBefore(String status,LocalDate limite); }
