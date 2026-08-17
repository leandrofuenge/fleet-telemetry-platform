package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.entity.PosicaoAtual;

@Repository
public interface PosicaoAtualRepository extends JpaRepository<PosicaoAtual, Long> {
    
    Optional<PosicaoAtual> findByVeiculoId(Long veiculoId);

    /**
     * SÊNIOR: Update cirúrgico in-place para controle de transição de zona.
     * Atualiza apenas a coluna necessária sem passar pelo fluxo pesado de persistência do JPA.
     */
    @Modifying
    @Transactional
    @Query("UPDATE PosicaoAtual p SET p.zonaAtual = :zona WHERE p.veiculoId = :veiculoId")
    int atualizarZonaAtual(@Param("veiculoId") Long veiculoId, @Param("zona") String zona);

    // RF06 RN-POS-001: UPSERT nativo PostgreSQL (mais rápido que save())
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO posicao_atual 
        (veiculo_id, tenant_id, veiculo_uuid, latitude, longitude, velocidade, direcao, ignicao, 
         status_veiculo, ultima_telemetria, ultima_atualizacao)
        VALUES 
        (:veiculoId, :tenantId, :veiculoUuid, :latitude, :longitude, :velocidade, :direcao, 
         :ignicao, :statusVeiculo, :ultimaTelemetria, NOW())
        ON CONFLICT (veiculo_id) DO UPDATE SET
            tenant_id = EXCLUDED.tenant_id,
            veiculo_uuid = EXCLUDED.veiculo_uuid,
            latitude = EXCLUDED.latitude,
            longitude = EXCLUDED.longitude,
            velocidade = EXCLUDED.velocidade,
            direcao = EXCLUDED.direcao,
            ignicao = EXCLUDED.ignicao,
            status_veiculo = EXCLUDED.status_veiculo,
            ultima_telemetria = EXCLUDED.ultima_telemetria,
            ultima_atualizacao = NOW()
        """, nativeQuery = true)
    void upsertPosicaoAtual(@Param("veiculoId") Long veiculoId,
                          @Param("tenantId") Long tenantId,
                          @Param("veiculoUuid") String veiculoUuid,
                          @Param("latitude") Double latitude,
                          @Param("longitude") Double longitude,
                          @Param("velocidade") Double velocidade,
                          @Param("direcao") Double direcao,
                          @Param("ignicao") Boolean ignicao,
                          @Param("statusVeiculo") String statusVeiculo,
                          @Param("ultimaTelemetria") LocalDateTime ultimaTelemetria);

    
    
    
	Page<Alerta> findUltimaPosicaoByVeiculoId(Long veiculoId);
	
	
	
	
	
	
}
