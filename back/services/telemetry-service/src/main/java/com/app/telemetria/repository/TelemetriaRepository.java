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
    
    // Buscar últimas telemetrias de um veículo
    List<Telemetria> findByVeiculoOrderByDataHoraDesc(Veiculo veiculo);
    
    // =========================================
    // Método para buscar a última telemetria
    // =========================================
    @Query("SELECT t FROM Telemetria t WHERE t.veiculo = :veiculo ORDER BY t.dataHora DESC LIMIT 1")
    Optional<Telemetria> findUltimaTelemetriaByVeiculo(@Param("veiculo") Veiculo veiculo);
    
    // Buscar telemetrias em um período
    List<Telemetria> findByVeiculoAndDataHoraBetweenOrderByDataHoraAsc(
        Veiculo veiculo, 
        LocalDateTime inicio, 
        LocalDateTime fim
    );
    
    // Buscar telemetrias recentes (últimos 5 minutos)
    @Query("SELECT t FROM Telemetria t WHERE t.veiculo = :veiculo AND t.dataHora >= :data ORDER BY t.dataHora DESC")
    List<Telemetria> findRecentByVeiculo(
        @Param("veiculo") Veiculo veiculo, 
        @Param("data") LocalDateTime data
    );
    
    // Método adicional útil: contar telemetrias de um veículo
    long countByVeiculo(Veiculo veiculo);
    
    // Método adicional útil: deletar telemetrias antigas
    void deleteByDataHoraBefore(LocalDateTime data);
}