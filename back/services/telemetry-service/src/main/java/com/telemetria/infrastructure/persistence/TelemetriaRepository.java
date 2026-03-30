package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Telemetria;

@Repository
public interface TelemetriaRepository extends JpaRepository<Telemetria, Long> {

    // =========================================
    // Métodos por ID do veículo
    // =========================================

    @Query(value = "SELECT * FROM telemetria WHERE veiculo_id = :veiculoId ORDER BY data_hora DESC",
           nativeQuery = true)
    List<Telemetria> findByVeiculoOrderByDataHoraDesc(@Param("veiculoId") Long veiculoId);

    @Query(value = "SELECT * FROM telemetria WHERE veiculo_id = :veiculoId ORDER BY data_hora DESC LIMIT 1",
           nativeQuery = true)
    Optional<Telemetria> findUltimaTelemetriaByVeiculo(@Param("veiculoId") Long veiculoId);

    @Query(value = "SELECT * FROM telemetria " +
                   "WHERE veiculo_id = :veiculoId " +
                   "AND data_hora BETWEEN :inicio AND :fim " +
                   "ORDER BY data_hora ASC",
           nativeQuery = true)
    List<Telemetria> findByVeiculoAndDataHoraBetweenOrderByDataHoraAsc(
            @Param("veiculoId") Long veiculoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query(value = "SELECT * FROM telemetria " +
                   "WHERE veiculo_id = :veiculoId AND data_hora >= :data " +
                   "ORDER BY data_hora DESC",
           nativeQuery = true)
    List<Telemetria> findRecentByVeiculo(
            @Param("veiculoId") Long veiculoId,
            @Param("data") LocalDateTime data);

    @Query(value = "SELECT COUNT(*) FROM telemetria WHERE veiculo_id = :veiculoId",
           nativeQuery = true)
    long countByVeiculo(@Param("veiculoId") Long veiculoId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM telemetria WHERE data_hora < :data",
           nativeQuery = true)
    void deleteByDataHoraBefore(@Param("data") LocalDateTime data);

    // =========================================
    // Aliases mantendo assinatura com ID
    // =========================================

    @Query(value = "SELECT * FROM telemetria WHERE veiculo_id = :veiculoId ORDER BY data_hora DESC LIMIT 1",
           nativeQuery = true)
    Optional<Telemetria> findUltimaTelemetriaByVeiculoId(@Param("veiculoId") Long veiculoId);

    @Query(value = "SELECT * FROM telemetria WHERE veiculo_id = :veiculoId ORDER BY data_hora DESC",
           nativeQuery = true)
    List<Telemetria> findByVeiculoIdOrderByDataHoraDesc(@Param("veiculoId") Long veiculoId);

    @Query(value = "SELECT * FROM telemetria " +
                   "WHERE veiculo_id = :veiculoId " +
                   "AND data_hora BETWEEN :inicio AND :fim " +
                   "ORDER BY data_hora ASC",
           nativeQuery = true)
    List<Telemetria> findByVeiculoIdAndDataHoraBetween(
            @Param("veiculoId") Long veiculoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query(value = "SELECT COUNT(*) FROM telemetria WHERE veiculo_id = :veiculoId",
           nativeQuery = true)
    long countByVeiculoId(@Param("veiculoId") Long veiculoId);
    
    
    Optional<Telemetria> findTopByVeiculoIdAndDeviceIdOrderByDataHoraDesc(Long veiculoId, String deviceId);
    
    @Query(value = "SELECT * FROM telemetria " +
                   "WHERE veiculo_id = :veiculoId " +
                   "AND data_hora < :dataHora " +
                   "ORDER BY data_hora DESC LIMIT 1",
           nativeQuery = true)
    Optional<Telemetria> findUltimaTelemetriaAntes(@Param("veiculoId") Long veiculoId, 
                                                  @Param("dataHora") LocalDateTime dataHora);
 // ADICIONAR estes 2 métodos no final da interface:

 // ✅ RF06 - Veículos sem sinal >30min COM ignição
 @Query(value = """
     SELECT DISTINCT t.veiculo_id 
     FROM telemetria t 
     JOIN posicao_atual pa ON t.veiculo_id = pa.veiculo_id 
     WHERE TIMESTAMPDIFF(MINUTE, t.data_hora, NOW()) > :minutosSemSinal
       AND pa.ignicao = :ignicaoOn
       AND pa.status_veiculo != 'DESCONHECIDO'
     """, nativeQuery = true)
 List<Long> findVeiculosSemSinal(@Param("minutosSemSinal") int minutosSemSinal, 
                                @Param("ignicaoOn") Boolean ignicaoOn);

 // ✅ RF06 - Status DESCONHECIDO após 5min
 @Modifying
 @Query(value = """
     UPDATE posicao_atual 
     SET status_veiculo = 'DESCONHECIDO' 
     WHERE TIMESTAMPDIFF(MINUTE, ultima_telemetria, NOW()) > 5
     """, nativeQuery = true)
 int atualizarStatusDesconhecido();

 @Modifying
 @Query(value = """
     DELETE FROM telemetria 
     WHERE veiculo_id = :veiculoId 
       AND data_hora < :dataLimite 
       AND (preservar_dados IS NULL OR preservar_dados = FALSE)
       AND tipo NOT IN ('JORNADA_LEI_12619')
     """, nativeQuery = true)
 int deleteByVeiculoIdAndDataHoraBeforeAndPreservarDadosFalse(
     @Param("veiculoId") Long veiculoId, 
     @Param("dataLimite") LocalDateTime dataLimite);

}