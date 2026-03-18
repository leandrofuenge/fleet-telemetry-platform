package com.app.telemetria.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.DesvioRota;

@Repository
public interface DesvioRotaRepository extends JpaRepository<DesvioRota, Long> {

    // ================ MÉTODOS COM SQL NATIVO ================

    @Query(value = "SELECT * FROM desvios_rota WHERE rota_id = :rotaId ORDER BY data_hora_desvio DESC",
           nativeQuery = true)
    List<DesvioRota> findByRotaIdOrderByDataHoraDesvioDesc(@Param("rotaId") Long rotaId);

    @Query(value = "SELECT * FROM desvios_rota WHERE veiculo_id = :veiculoId ORDER BY data_hora_desvio DESC",
           nativeQuery = true)
    List<DesvioRota> findByVeiculoIdOrderByDataHoraDesvioDesc(@Param("veiculoId") Long veiculoId);

    @Query(value = "SELECT * FROM desvios_rota WHERE resolvido = FALSE",
           nativeQuery = true)
    List<DesvioRota> findByResolvidoFalse();

    @Query(value = "SELECT * FROM desvios_rota WHERE resolvido = TRUE",
           nativeQuery = true)
    List<DesvioRota> findByResolvidoTrue();

    @Query(value = "SELECT * FROM desvios_rota WHERE rota_id = :rotaId AND resolvido = FALSE LIMIT 1",
           nativeQuery = true)
    Optional<DesvioRota> findByRotaIdAndResolvidoFalse(@Param("rotaId") Long rotaId);

    @Query(value = "SELECT * FROM desvios_rota WHERE veiculo_id = :veiculoId AND resolvido = FALSE LIMIT 1",
           nativeQuery = true)
    Optional<DesvioRota> findByVeiculoIdAndResolvidoFalse(@Param("veiculoId") Long veiculoId);

    // ================ MÉTODOS DE CONSULTA ESPECÍFICOS ================

    @Query(value = "SELECT * FROM desvios_rota WHERE rota_id = :rotaId AND resolvido = FALSE ORDER BY data_hora_desvio DESC",
           nativeQuery = true)
    List<DesvioRota> findDesviosAtivosPorRota(@Param("rotaId") Long rotaId);

    // ================ MÉTODOS DE CONTAGEM COM SQL NATIVO ================

    @Query(value = "SELECT COUNT(*) FROM desvios_rota WHERE veiculo_id = :veiculoId AND resolvido = FALSE",
           nativeQuery = true)
    long countDesviosAtivosPorVeiculo(@Param("veiculoId") Long veiculoId);
}