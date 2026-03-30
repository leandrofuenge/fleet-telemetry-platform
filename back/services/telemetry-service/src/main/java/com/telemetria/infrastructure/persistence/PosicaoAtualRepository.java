package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.PosicaoAtual;

@Repository
public interface PosicaoAtualRepository extends JpaRepository<PosicaoAtual, Long> {
    
    Optional<PosicaoAtual> findByVeiculoId(Long veiculoId);

    // ✅ RF06 RN-POS-001: UPSERT nativo MySQL (mais rápido que save())
    @Query(value = """
        INSERT INTO posicao_atual 
        (veiculo_id, tenant_id, veiculo_uuid, latitude, longitude, velocidade, direcao, ignicao, 
         status_veiculo, ultima_telemetria, ultima_atualizacao)
        VALUES 
        (:veiculoId, :tenantId, :veiculoUuid, :latitude, :longitude, :velocidade, :direcao, 
         :ignicao, :statusVeiculo, :ultimaTelemetria, NOW())
        ON DUPLICATE KEY UPDATE
            latitude = VALUES(latitude),
            longitude = VALUES(longitude),
            velocidade = VALUES(velocidade),
            direcao = VALUES(direcao),
            ignicao = VALUES(ignicao),
            status_veiculo = VALUES(status_veiculo),
            ultima_telemetria = VALUES(ultima_telemetria),
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
}