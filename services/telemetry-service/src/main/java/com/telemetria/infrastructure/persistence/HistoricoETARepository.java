// src/main/java/com/telemetria/infrastructure/persistence/HistoricoETARepository.java
package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.HistoricoETA;

@Repository
public interface HistoricoETARepository extends JpaRepository<HistoricoETA, Long> {
    
    // =========================================
    // Métodos derivados (Spring Data JPA)
    // =========================================
    
    /**
     * Busca todos os históricos de ETA de uma viagem ordenados por data de cálculo decrescente
     */
    List<HistoricoETA> findByViagemIdOrderByDataCalculoDesc(Long viagemId);
    
    /**
     * Busca o histórico de ETA mais recente de uma viagem
     * Usando o padrão "Top" do Spring Data JPA
     */
    Optional<HistoricoETA> findTopByViagemIdOrderByDataCalculoDesc(Long viagemId);
    
    /**
     * Busca o histórico de ETA mais recente de um veículo
     */
    Optional<HistoricoETA> findTopByVeiculoIdOrderByDataCalculoDesc(Long veiculoId);
    
    /**
     * Busca o histórico de ETA mais recente com status específico
     */
    Optional<HistoricoETA> findTopByViagemIdAndStatusEtaOrderByDataCalculoDesc(Long viagemId, String statusEta);
    
    // =========================================
    // Métodos com @Query (JPQL)
    // =========================================
    
    /**
     * Busca históricos de ETA de uma viagem a partir de uma data específica
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.viagemId = :viagemId AND h.dataCalculo >= :inicio ORDER BY h.dataCalculo DESC")
    List<HistoricoETA> findByViagemIdAndDataCalculoAfter(@Param("viagemId") Long viagemId, 
                                                          @Param("inicio") LocalDateTime inicio);
    
    /**
     * Busca o último histórico de ETA de uma viagem (JPQL)
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.viagemId = :viagemId ORDER BY h.dataCalculo DESC")
    Optional<HistoricoETA> findUltimoByViagemId(@Param("viagemId") Long viagemId);
    
    /**
     * Busca o último histórico de ETA de um veículo (JPQL)
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.veiculoId = :veiculoId ORDER BY h.dataCalculo DESC")
    Optional<HistoricoETA> findUltimoByVeiculoId(@Param("veiculoId") Long veiculoId);
    
    /**
     * Busca históricos com status INDETERMINADO recentes
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.statusEta = 'INDETERMINADO' AND h.dataCalculo >= :limite ORDER BY h.dataCalculo DESC")
    List<HistoricoETA> findIndeterminadosRecentes(@Param("limite") LocalDateTime limite);
    
    /**
     * Busca históricos com atraso superior a X minutos
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.atrasoMinutos >= :minutosAtraso AND h.dataCalculo >= :dataInicio ORDER BY h.atrasoMinutos DESC")
    List<HistoricoETA> findByAtrasoMinutosGreaterThanEqual(@Param("minutosAtraso") Long minutosAtraso, 
                                                            @Param("dataInicio") LocalDateTime dataInicio);
    
    /**
     * Busca o primeiro histórico de ETA de uma viagem
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.viagemId = :viagemId ORDER BY h.dataCalculo ASC")
    Optional<HistoricoETA> findPrimeiroByViagemId(@Param("viagemId") Long viagemId);
    
    /**
     * Conta quantos registros de ETA uma viagem possui
     */
    @Query("SELECT COUNT(h) FROM HistoricoETA h WHERE h.viagemId = :viagemId")
    long countByViagemId(@Param("viagemId") Long viagemId);
    
    /**
     * Busca históricos de ETA em um período específico
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.dataCalculo BETWEEN :inicio AND :fim ORDER BY h.dataCalculo DESC")
    List<HistoricoETA> findByDataCalculoBetween(@Param("inicio") LocalDateTime inicio, 
                                                 @Param("fim") LocalDateTime fim);
    
    /**
     * Busca o último ETA válido (não indeterminado) de uma viagem
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.viagemId = :viagemId AND h.statusEta != 'INDETERMINADO' ORDER BY h.dataCalculo DESC")
    Optional<HistoricoETA> findUltimoEtaValidoByViagemId(@Param("viagemId") Long viagemId);
    
    /**
     * Busca viagens com ETA indeterminado ativo
     */
    @Query("SELECT DISTINCT h.viagemId FROM HistoricoETA h WHERE h.statusEta = 'INDETERMINADO' AND h.dataCalculo >= :limite")
    List<Long> findViagensComEtaIndeterminado(@Param("limite") LocalDateTime limite);
    
    /**
     * Busca o último histórico de ETA de uma viagem com limite de linhas (SQL nativo)
     */
    @Query(value = "SELECT * FROM historico_eta WHERE viagem_id = :viagemId ORDER BY data_calculo DESC LIMIT 1", nativeQuery = true)
    Optional<HistoricoETA> findUltimoByViagemIdNative(@Param("viagemId") Long viagemId);
    
    /**
     * Busca o último histórico de ETA de um veículo (SQL nativo)
     */
    @Query(value = "SELECT * FROM historico_eta WHERE veiculo_id = :veiculoId ORDER BY data_calculo DESC LIMIT 1", nativeQuery = true)
    Optional<HistoricoETA> findUltimoByVeiculoIdNative(@Param("veiculoId") Long veiculoId);
    
    /**
     * Busca o histórico de ETA mais recente para cada viagem ativa
     */
    @Query(value = """
        SELECT h.* FROM historico_eta h
        INNER JOIN (
            SELECT viagem_id, MAX(data_calculo) as max_data
            FROM historico_eta
            WHERE viagem_id IN :viagemIds
            GROUP BY viagem_id
        ) latest ON h.viagem_id = latest.viagem_id AND h.data_calculo = latest.max_data
        """, nativeQuery = true)
    List<HistoricoETA> findUltimosEtaPorViagem(@Param("viagemIds") List<Long> viagemIds);
    
    /**
     * Deleta históricos de ETA antigos (para limpeza de dados)
     */
    @Query("DELETE FROM HistoricoETA h WHERE h.dataCalculo < :dataLimite")
    void deleteByDataCalculoBefore(@Param("dataLimite") LocalDateTime dataLimite);
    
    /**
     * Busca históricos com status específico
     */
    List<HistoricoETA> findByStatusEtaOrderByDataCalculoDesc(String statusEta);
    
    /**
     * Busca históricos de uma viagem com atraso
     */
    @Query("SELECT h FROM HistoricoETA h WHERE h.viagemId = :viagemId AND h.atrasoMinutos > 0 ORDER BY h.dataCalculo DESC")
    List<HistoricoETA> findAtrasosByViagemId(@Param("viagemId") Long viagemId);
    
    /**
     * Busca o maior atraso registrado para uma viagem
     */
    @Query("SELECT MAX(h.atrasoMinutos) FROM HistoricoETA h WHERE h.viagemId = :viagemId")
    Long findMaxAtrasoByViagemId(@Param("viagemId") Long viagemId);
}