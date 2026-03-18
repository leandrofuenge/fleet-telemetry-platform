package com.app.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.entity.Viagem;

@Repository
public interface ViagemRepository extends JpaRepository<Viagem, Long> {
    
    // ================ MÉTODOS COM SQL NATIVO ================
    
    @Query(value = "SELECT * FROM viagens WHERE veiculo_id = :veiculoId ORDER BY data_saida DESC", 
           nativeQuery = true)
    List<Viagem> findByVeiculoIdOrderByDataSaidaDesc(@Param("veiculoId") Long veiculoId);
    
    @Query(value = "SELECT * FROM viagens WHERE motorista_id = :motoristaId ORDER BY data_saida DESC", 
           nativeQuery = true)
    List<Viagem> findByMotoristaIdOrderByDataSaidaDesc(@Param("motoristaId") Long motoristaId);
    
    @Query(value = "SELECT * FROM viagens WHERE status = :status ORDER BY data_saida DESC", 
           nativeQuery = true)
    List<Viagem> findByStatus(@Param("status") String status);
    
    @Query(value = "SELECT * FROM viagens WHERE veiculo_id = :veiculoId AND status = :status ORDER BY data_inicio DESC LIMIT 1", 
           nativeQuery = true)
    Optional<Viagem> findByVeiculoIdAndStatus(
            @Param("veiculoId") Long veiculoId, 
            @Param("status") String status);
    
    // Método corrigido - agora aceita Long (veiculoId)
    @Query(value = "SELECT * FROM viagens WHERE veiculo_id = :veiculoId AND status = :status ORDER BY data_inicio DESC LIMIT 1", 
           nativeQuery = true)
    Optional<Viagem> findByVeiculoAndStatus(
            @Param("veiculoId") Long veiculoId, 
            @Param("status") String status);
    
    // Método de conveniência que aceita objeto Veiculo
    default Optional<Viagem> findByVeiculoAndStatus(Veiculo veiculo, String status) {
        if (veiculo == null || veiculo.getId() == null) {
            return Optional.empty();
        }
        return findByVeiculoAndStatus(veiculo.getId(), status);
    }
    
    @Query(value = "SELECT * FROM viagens WHERE status = 'EM_ANDAMENTO' ORDER BY data_inicio DESC", 
           nativeQuery = true)
    List<Viagem> findAllEmAndamento();
    
    @Query(value = "SELECT * FROM viagens WHERE data_chegada_prevista < :agora AND status != 'FINALIZADA' ORDER BY data_chegada_prevista ASC", 
           nativeQuery = true)
    List<Viagem> findAtrasadas(@Param("agora") LocalDateTime agora);
    
    @Query(value = "SELECT COUNT(*) FROM viagens WHERE status = :status", 
           nativeQuery = true)
    long countByStatus(@Param("status") String status);
    
    @Query(value = "SELECT * FROM viagens WHERE veiculo_id = :veiculoId AND status = :status ORDER BY data_inicio DESC", 
           nativeQuery = true)
    Optional<Viagem> findByVeiculoIdAndStatusOrderByDataInicioDesc(
            @Param("veiculoId") Long veiculoId, 
            @Param("status") String status);
    
    @Query(value = "SELECT * FROM viagens WHERE motorista_id = :motoristaId AND status = :status ORDER BY data_inicio DESC LIMIT 1", 
           nativeQuery = true)
    Optional<Viagem> findByMotoristaIdAndStatus(
            @Param("motoristaId") Long motoristaId, 
            @Param("status") String status);
}