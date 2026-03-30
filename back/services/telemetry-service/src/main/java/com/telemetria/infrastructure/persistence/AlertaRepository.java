package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    // ================ MÉTODOS COM SQL NATIVO ================
    
    @Query(value = "SELECT * FROM alertas WHERE veiculo_id = :veiculoId ORDER BY data_hora DESC", 
           nativeQuery = true)
    List<Alerta> findByVeiculoIdOrderByDataHoraDesc(@Param("veiculoId") Long veiculoId);
    
    @Query(value = "SELECT * FROM alertas WHERE motorista_id = :motoristaId ORDER BY data_hora DESC", 
           nativeQuery = true)
    List<Alerta> findByMotoristaIdOrderByDataHoraDesc(@Param("motoristaId") Long motoristaId);
    
    @Query(value = "SELECT * FROM alertas WHERE viagem_id = :viagemId ORDER BY data_hora DESC", 
           nativeQuery = true)
    List<Alerta> findByViagemIdOrderByDataHoraDesc(@Param("viagemId") Long viagemId);
    
    @Query(value = "SELECT * FROM alertas WHERE resolvido = FALSE ORDER BY data_hora DESC", 
           nativeQuery = true)
    List<Alerta> findByResolvidoFalseOrderByDataHoraDesc();
    
    @Query(value = "SELECT * FROM alertas WHERE severidade = :severidade AND resolvido = FALSE ORDER BY data_hora DESC", 
           nativeQuery = true)
    List<Alerta> findBySeveridadeAndResolvidoFalseOrderByDataHoraDesc(@Param("severidade") String severidade);
    
    @Query(value = "SELECT * FROM alertas WHERE data_hora BETWEEN :inicio AND :fim ORDER BY data_hora DESC", 
           nativeQuery = true)
    List<Alerta> findByDataHoraBetweenOrderByDataHoraDesc(
            @Param("inicio") LocalDateTime inicio, 
            @Param("fim") LocalDateTime fim);
    
    @Query(value = "SELECT * FROM alertas WHERE veiculo_id = :veiculoId AND tipo = :tipo AND resolvido = FALSE ORDER BY data_hora DESC LIMIT 1", 
           nativeQuery = true)
    Optional<Alerta> findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(
            @Param("veiculoId") Long veiculoId,
            @Param("tipo") String tipo);
    
    @Query(value = "SELECT COUNT(*) > 0 FROM alertas WHERE veiculo_id = :veiculoId AND tipo = :tipo AND resolvido = FALSE", 
           nativeQuery = true)
    boolean existsByVeiculoIdAndTipoAndResolvidoFalse(
            @Param("veiculoId") Long veiculoId,
            @Param("tipo") String tipo);
    
    @Query(value = "SELECT * FROM alertas WHERE veiculo_id = :veiculoId AND tipo = :tipo AND resolvido = FALSE ORDER BY data_hora DESC", 
           nativeQuery = true)
    List<Alerta> findByVeiculoIdAndTipoAndResolvidoFalseOrderByDataHoraDesc(
            @Param("veiculoId") Long veiculoId,
            @Param("tipo") String tipo);
    
    // ================ MÉTODOS DE CONTAGEM COM SQL NATIVO ================
    
    @Query(value = "SELECT COUNT(*) FROM alertas WHERE resolvido = FALSE", 
           nativeQuery = true)
    long countByResolvidoFalse();
    
    @Query(value = "SELECT COUNT(*) FROM alertas WHERE severidade = :severidade AND resolvido = FALSE", 
           nativeQuery = true)
    long countBySeveridadeAndResolvidoFalse(@Param("severidade") String severidade);
    
    // ================ MÉTODOS DE CONVENIÊNCIA COM ENUM ================
    
    default boolean existsByVeiculoIdAndTipoAndResolvidoFalse(Long veiculoId, TipoAlerta tipo) {
        return existsByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, tipo.name());
    }
    
    default Optional<Alerta> findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(Long veiculoId, TipoAlerta tipo) {
        return findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(veiculoId, tipo.name());
    }
    
    default List<Alerta> findByVeiculoIdAndTipoAndResolvidoFalseOrderByDataHoraDesc(Long veiculoId, TipoAlerta tipo) {
        return findByVeiculoIdAndTipoAndResolvidoFalseOrderByDataHoraDesc(veiculoId, tipo.name());
    }
    
    default List<Alerta> findBySeveridadeAndResolvidoFalseOrderByDataHoraDesc(SeveridadeAlerta severidade) {
        return findBySeveridadeAndResolvidoFalseOrderByDataHoraDesc(severidade.name());
    }
}