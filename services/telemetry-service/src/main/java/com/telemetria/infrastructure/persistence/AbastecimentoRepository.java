package com.telemetria.infrastructure.persistence; import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Abastecimento;
public interface AbastecimentoRepository extends JpaRepository<Abastecimento,Long>{ List<Abastecimento> findByVeiculoIdAndDataHoraBetween(Long id,LocalDateTime inicio,LocalDateTime fim); List<Abastecimento> findByStatusConciliacaoAndDataHoraBefore(String status,LocalDateTime limite); }
