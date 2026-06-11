package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;

import jakarta.transaction.Transactional;

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
    
    // Método que retorna count (usado internamente)
    @Query(value = "SELECT COUNT(*) FROM alertas WHERE veiculo_id = :veiculoId AND tipo = :tipo AND resolvido = FALSE", 
           nativeQuery = true)
    long countByVeiculoIdAndTipoAndResolvidoFalse(@Param("veiculoId") Long veiculoId, @Param("tipo") String tipo);
    
    // Método que retorna Optional (já existente)
    @Query(value = "SELECT * FROM alertas WHERE veiculo_id = :veiculoId AND tipo = :tipo AND resolvido = FALSE ORDER BY data_hora DESC LIMIT 1", 
           nativeQuery = true)
    Optional<Alerta> findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(
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
        return countByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, tipo.name()) > 0;
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

	boolean existsByViagemIdAndTipo(Long id, TipoAlerta inicioViagem);

	Optional<Alerta> findPrimeiroByVeiculoIdAndTipoAndResolvidoFalse(Long veiculoId, TipoAlerta gpsSemSinal);

	boolean existsByViagemIdAndTipoAndResolvidoFalse(Long id, TipoAlerta tempoDirecao);
	
	
	@Modifying // Informa ao Spring Data que é uma query de escrita (UPDATE/DELETE)
    @Query("UPDATE Alerta a SET a.resolvido = true, a.dataHoraResolucao = :dataResolucao " +
           "WHERE a.veiculoId = :veiculoId AND a.tipo = :tipo AND a.resolvido = false")
    int resolverAlertasAtivos(@Param("veiculoId") Long veiculoId, 
                             @Param("tipo") TipoAlerta tipo, 
                             @Param("dataResolucao") LocalDateTime dataResolucao);
	
	
	boolean existsByVeiculoIdAndTipoAndResolvidoFalseAndMensagemContaining(
		    Long veiculoId, 
		    TipoAlerta tipo, 
		    String trechoMensagem
		);
	
	
	boolean existsByMotoristaIdAndTipoAndSeveridadeAndResolvidoFalse(
		    Long motoristaId, 
		    TipoAlerta tipo, 
		    SeveridadeAlerta severidade
		);	
	
	
	@Modifying
	@Transactional
	@Query("UPDATE Alerta a SET a.resolvido = true, a.dataResolucao = NOW() " +
	       "WHERE a.motoristaId = :motoristaId AND a.tipo = :tipo AND a.resolvido = false")
	void resolverAlertasAtivosPorMotoristaETipo(
	    @Param("motoristaId") Long motoristaId, 
	    @Param("tipo") TipoAlerta tipo
	);

	boolean existsByMotoristaIdAndTipoAndResolvidoFalse(Long id, TipoAlerta cnhVencida);

	boolean existsByVeiculoIdAndTipoAndResolvidoFalseAndDataHoraAfter(Long veiculoId, TipoAlerta saltoPosicao,
			LocalDateTime limiteJanela);
	
	
	/**
     * SÊNIOR: Executa a resolução (fechamento) em lote de alertas ativos para um Veículo específico.
     * O uso do @Modifying garante uma operação de escrita (UPDATE) direta e de alta performance.
     */
    @Modifying
    @Transactional // Garante o commit da transação de escrita
    @Query("UPDATE Alerta a SET a.resolvido = true, a.dataResolucao = NOW() " +
           "WHERE a.veiculoId = :veiculoId AND a.tipo = :tipo AND a.resolvido = false")
    void resolverAlertasAtivosPorVeiculoETipo(
        @Param("veiculoId") Long veiculoId, 
        @Param("tipo") TipoAlerta tipo
    );
	
}