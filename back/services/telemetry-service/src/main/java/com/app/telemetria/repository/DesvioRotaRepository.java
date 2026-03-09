package com.app.telemetria.repository;

import com.app.telemetria.entity.DesvioRota;
import com.app.telemetria.entity.Rota;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DesvioRotaRepository extends JpaRepository<DesvioRota, Long> {
    List<DesvioRota> findByRotaIdOrderByDataHoraDesvioDesc(Long rotaId);
    List<DesvioRota> findByVeiculoIdOrderByDataHoraDesvioDesc(Long veiculoId);
    List<DesvioRota> findByResolvidoFalse();
    Optional<DesvioRota> findByRotaAndResolvidoFalse(Rota rota);
}