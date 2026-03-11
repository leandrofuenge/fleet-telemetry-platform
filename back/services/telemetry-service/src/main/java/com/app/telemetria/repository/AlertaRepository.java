package com.app.telemetria.repository;

import com.app.telemetria.entity.Alerta;
import com.app.telemetria.entity.Veiculo;
import com.app.telemetria.entity.Viagem;
import com.app.telemetria.enums.SeveridadeAlerta; // IMPORTANTE
import com.app.telemetria.enums.TipoAlerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

        Page<Alerta> findAll(Pageable pageable);

        // Métodos com Veiculo (entidade)
        List<Alerta> findByVeiculoOrderByDataHoraDesc(Veiculo veiculo);

        List<Alerta> findByMotoristaIdOrderByDataHoraDesc(Long motoristaId);

        List<Alerta> findByViagemOrderByDataHoraDesc(Viagem viagem);

        List<Alerta> findByDataHoraBetweenOrderByDataHoraDesc(LocalDateTime inicio, LocalDateTime fim);

        List<Alerta> findByViagemIdOrderByDataHoraDesc(Long viagemId);

        List<Alerta> findByResolvidoFalseOrderByDataHoraDesc();

        // CORREÇÃO AQUI: usar severidade em vez de gravidade
        List<Alerta> findBySeveridadeAndResolvidoFalseOrderByDataHoraDesc(SeveridadeAlerta severidade);

        @Query("SELECT a FROM Alerta a WHERE a.veiculo = :veiculo AND a.tipo = :tipo AND a.resolvido = false ORDER BY a.dataHora DESC")
        Optional<Alerta> findPrimeiroByVeiculoAndTipoOrderByDataHoraDesc(
                        @Param("veiculo") Veiculo veiculo,
                        @Param("tipo") String tipo);

        boolean existsByVeiculoAndTipoAndResolvidoFalse(Veiculo veiculo, String tipo);

        List<Alerta> findByVeiculoAndTipoAndResolvidoFalseOrderByDataHoraDesc(
                        Veiculo veiculo, String tipo);

        @Query("SELECT a FROM Alerta a WHERE a.dataHora BETWEEN :inicio AND :fim ORDER BY a.dataHora DESC")
        List<Alerta> findByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

        long countByResolvidoFalse();

        // CORREÇÃO AQUI
        long countBySeveridadeAndResolvidoFalse(SeveridadeAlerta severidade);

        // Métodos com Long veiculoId e Enum TipoAlerta
        List<Alerta> findByVeiculoIdOrderByDataHoraDesc(Long veiculoId);

        @Query("SELECT a FROM Alerta a WHERE a.veiculoId = :veiculoId AND a.tipo = :tipo AND a.resolvido = false ORDER BY a.dataHora DESC")
        Optional<Alerta> findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(
                        @Param("veiculoId") Long veiculoId,
                        @Param("tipo") TipoAlerta tipo);

        boolean existsByVeiculoIdAndTipoAndResolvidoFalse(@Param("veiculoId") Long veiculoId,
                        @Param("tipo") TipoAlerta tipo);

        List<Alerta> findByVeiculoIdAndTipoAndResolvidoFalseOrderByDataHoraDesc(
                        @Param("veiculoId") Long veiculoId, @Param("tipo") TipoAlerta tipo);
}