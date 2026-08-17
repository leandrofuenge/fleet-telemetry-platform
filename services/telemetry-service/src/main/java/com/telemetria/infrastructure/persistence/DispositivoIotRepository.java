package com.telemetria.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.enums.TipoDispositivo;

@Repository
public interface DispositivoIotRepository extends JpaRepository<DispositivoIot, Long> {

    Optional<DispositivoIot> findByDeviceId(String deviceId);
    
    List<DispositivoIot> findByVeiculoId(Long veiculoId);
    
    long countByVeiculoId(Long veiculoId);
    
    @Query("SELECT d FROM DispositivoIot d WHERE d.veiculoId = :veiculoId AND d.tipo = :tipo")
    Optional<DispositivoIot> findByVeiculoIdAndTipo(Long veiculoId, TipoDispositivo tipo);
    
    boolean existsByDeviceIdAndVeiculoIdNot(String deviceId, Long veiculoId);
    
    /**
     * Remove o vínculo de qualquer dispositivo anterior do veículo antes de associar o novo,
     * evitando duplicidade de telemetria ativa para o mesmo veículo.
     */
    @Modifying
    @Transactional
    @Query("UPDATE DispositivoIot d SET d.status = 'INATIVO', d.veiculoId = null " +
           "WHERE d.veiculoId = :veiculoId AND d.tipo = :tipo AND d.status = 'ATIVO'")
    void desvincularDispositivosAtivosPorVeiculo(
        @Param("veiculoId") Long veiculoId, 
        @Param("tipo") TipoDispositivo tipo
    );
     
	
}
