package com.app.telemetria.repository;

import com.app.telemetria.entity.Telemetria;
import com.app.telemetria.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TelemetriaRepository extends JpaRepository<Telemetria, Long> {

    // =========================================
    // Métodos por objeto Veiculo (já existentes)
    // =========================================

    List<Telemetria> findByVeiculoOrderByDataHoraDesc(Veiculo veiculo);

    @Query("SELECT t FROM Telemetria t WHERE t.veiculo = :veiculo ORDER BY t.dataHora DESC LIMIT 1")
    Optional<Telemetria> findUltimaTelemetriaByVeiculo(@Param("veiculo") Veiculo veiculo);

    List<Telemetria> findByVeiculoAndDataHoraBetweenOrderByDataHoraAsc(
            Veiculo veiculo,
            LocalDateTime inicio,
            LocalDateTime fim);

    @Query("SELECT t FROM Telemetria t WHERE t.veiculo = :veiculo AND t.dataHora >= :data ORDER BY t.dataHora DESC")
    List<Telemetria> findRecentByVeiculo(
            @Param("veiculo") Veiculo veiculo,
            @Param("data") LocalDateTime data);

    long countByVeiculo(Veiculo veiculo);

    void deleteByDataHoraBefore(LocalDateTime data);

    // =========================================
    // Métodos adicionais por ID do veículo
    // =========================================

    /**
     * Busca a última telemetria de um veículo pelo seu ID.
     * Útil para evitar carregar a entidade Veiculo quando só se tem o ID.
     */
    @Query("SELECT t FROM Telemetria t WHERE t.veiculo.id = :veiculoId ORDER BY t.dataHora DESC LIMIT 1")
    Optional<Telemetria> findUltimaTelemetriaByVeiculoId(@Param("veiculoId") Long veiculoId);

    /**
     * Lista telemetrias de um veículo ordenadas pela data/hora descendente.
     */
    List<Telemetria> findByVeiculoIdOrderByDataHoraDesc(Long veiculoId);

    /**
     * Busca telemetrias de um veículo em um período, usando o ID do veículo.
     */
    @Query("SELECT t FROM Telemetria t WHERE t.veiculo.id = :veiculoId AND t.dataHora BETWEEN :inicio AND :fim ORDER BY t.dataHora ASC")
    List<Telemetria> findByVeiculoIdAndDataHoraBetween(
            @Param("veiculoId") Long veiculoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    /**
     * Conta quantas telemetrias um veículo possui.
     */
    long countByVeiculoId(Long veiculoId);
}