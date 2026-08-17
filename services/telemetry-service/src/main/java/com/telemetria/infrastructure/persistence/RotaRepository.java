package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.Rota;

@Repository
public interface RotaRepository extends JpaRepository<Rota, Long> {

    @Query(value = "SELECT * FROM rotas WHERE status = :status",
           nativeQuery = true)
    List<Rota> findByStatus(@Param("status") String status);

    @Query(value = "SELECT * FROM rotas WHERE veiculo_id = :veiculoId AND status = :status",
           nativeQuery = true)
    List<Rota> findByVeiculoIdAndStatus(
            @Param("veiculoId") Long veiculoId,
            @Param("status") String status);

    @Query(value = "SELECT * FROM rotas WHERE data_inicio BETWEEN :inicio AND :fim",
           nativeQuery = true)
    List<Rota> findByDataInicioBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query(value = "SELECT * FROM rotas WHERE ativa = TRUE",
           nativeQuery = true)
    List<Rota> findByAtivaTrue();

    @Query(value = "SELECT * FROM rotas WHERE veiculo_id = :veiculoId",
           nativeQuery = true)
    List<Rota> findByVeiculoId(@Param("veiculoId") Long veiculoId);

    @Query(value = "SELECT * FROM rotas WHERE motorista_id = :motoristaId",
           nativeQuery = true)
    List<Rota> findByMotoristaId(@Param("motoristaId") Long motoristaId);

    @Query(value = "SELECT * FROM rotas WHERE nome = :nome LIMIT 1",
           nativeQuery = true)
    Optional<Rota> findByNome(@Param("nome") String nome);

    // ========== SOLUÇÃO 2 APLICADA ==========
    @Query(value = "SELECT COUNT(*) FROM rotas WHERE nome = :nome",
           nativeQuery = true)
    Long countByNome(@Param("nome") String nome);

    @Query(value = "SELECT COUNT(*) FROM rotas WHERE nome = :nome AND id != :id",
           nativeQuery = true)
    Long countByNomeAndIdNot(@Param("nome") String nome, @Param("id") Long id);
    // =======================================

    @Query(value = "SELECT * FROM rotas WHERE nome LIKE CONCAT('%', :nome, '%')",
           nativeQuery = true)
    List<Rota> findByNomeContainingIgnoreCase(@Param("nome") String nome);

    @Query(value = "SELECT * FROM rotas " +
                   "WHERE origem LIKE CONCAT('%', :origem, '%') " +
                   "AND destino LIKE CONCAT('%', :destino, '%')",
           nativeQuery = true)
    List<Rota> findByOrigemContainingIgnoreCaseAndDestinoContainingIgnoreCase(
            @Param("origem") String origem,
            @Param("destino") String destino);

    @Query(value = "SELECT * FROM rotas WHERE ativa = TRUE ORDER BY created_at DESC",
           nativeQuery = true)
    List<Rota> findByAtivaTrueOrderByCreatedAtDesc();

    @Query(value = "SELECT * FROM rotas WHERE status = :status ORDER BY created_at DESC",
           nativeQuery = true)
    List<Rota> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query(value = "SELECT COUNT(*) FROM rotas WHERE veiculo_id = :veiculoId AND ativa = TRUE",
           nativeQuery = true)
    long countRotasAtivasPorVeiculo(@Param("veiculoId") Long veiculoId);

    @Query(value = "SELECT * FROM rotas WHERE distancia_prevista > :distancia",
           nativeQuery = true)
    List<Rota> findByDistanciaPrevistaGreaterThan(@Param("distancia") Double distancia);

    @Query(value = "SELECT * FROM rotas WHERE tempo_previsto < :tempo",
           nativeQuery = true)
    List<Rota> findByTempoPrevistoLessThan(@Param("tempo") Integer tempo);
}