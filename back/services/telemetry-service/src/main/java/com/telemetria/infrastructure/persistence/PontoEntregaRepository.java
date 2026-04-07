package com.telemetria.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.PontoEntrega;
import com.telemetria.domain.enums.StatusPontoEntrega;

/**
 * Repositório para PontoEntrega
 * RF11 — Pontos de Entrega e Proof of Delivery
 */
@Repository
public interface PontoEntregaRepository extends JpaRepository<PontoEntrega, Long> {

    /**
     * Busca pontos de entrega por viagem
     */
    List<PontoEntrega> findByViagemIdOrderByOrdemAsc(Long viagemId);

    /**
     * Busca pontos de entrega por viagem com paginação
     */
    Page<PontoEntrega> findByViagemId(Long viagemId, Pageable pageable);

    /**
     * Busca pontos de entrega por rota
     */
    List<PontoEntrega> findByRotaIdOrderByOrdemAsc(Long rotaId);

    /**
     * Busca pontos por status
     */
    List<PontoEntrega> findByViagemIdAndStatus(Long viagemId, StatusPontoEntrega status);

    /**
     * Busca pontos pendentes de uma viagem
     */
    @Query("SELECT p FROM PontoEntrega p WHERE p.viagemId = :viagemId AND p.status = 'PENDENTE' ORDER BY p.ordem ASC")
    List<PontoEntrega> findPendentesByViagemId(@Param("viagemId") Long viagemId);

    /**
     * Busca próximo ponto pendente da viagem
     */
    @Query("SELECT p FROM PontoEntrega p WHERE p.viagemId = :viagemId AND p.status = 'PENDENTE' ORDER BY p.ordem ASC LIMIT 1")
    Optional<PontoEntrega> findProximoPendente(@Param("viagemId") Long viagemId);

    /**
     * Conta total de pontos de uma viagem
     */
    @Query("SELECT COUNT(p) FROM PontoEntrega p WHERE p.viagemId = :viagemId")
    long countByViagemId(@Param("viagemId") Long viagemId);

    /**
     * Conta pontos por status
     */
    long countByViagemIdAndStatus(Long viagemId, StatusPontoEntrega status);

    /**
     * Conta pontos entregues
     */
    @Query("SELECT COUNT(p) FROM PontoEntrega p WHERE p.viagemId = :viagemId AND p.status = 'ENTREGUE'")
    long countEntreguesByViagemId(@Param("viagemId") Long viagemId);

    /**
     * Conta pontos falhos
     */
    @Query("SELECT COUNT(p) FROM PontoEntrega p WHERE p.viagemId = :viagemId AND p.status = 'FALHOU'")
    long countFalhosByViagemId(@Param("viagemId") Long viagemId);

    /**
     * Busca pontos de entrega (tipo = ENTREGA) de uma viagem
     */
    @Query("SELECT p FROM PontoEntrega p WHERE p.viagemId = :viagemId AND p.tipo = 'ENTREGA' ORDER BY p.ordem ASC")
    List<PontoEntrega> findEntregasByViagemId(@Param("viagemId") Long viagemId);

    /**
     * Busca pontos sem proof of delivery (assinatura ou foto)
     * RF11: Verificação de compliance
     */
    @Query("SELECT p FROM PontoEntrega p WHERE p.viagemId = :viagemId AND p.tipo = 'ENTREGA' " +
           "AND p.status = 'ENTREGUE' AND p.assinaturaPath IS NULL AND p.fotoEntregaPath IS NULL")
    List<PontoEntrega> findEntregasSemProofOfDelivery(@Param("viagemId") Long viagemId);

    /**
     * Busca pontos com ocorrência preenchida (status FALHOU)
     */
    @Query("SELECT p FROM PontoEntrega p WHERE p.viagemId = :viagemId AND p.status = 'FALHOU' AND p.ocorrencia IS NOT NULL")
    List<PontoEntrega> findFalhasComOcorrencia(@Param("viagemId") Long viagemId);

    /**
     * Busca pontos falhos sem ocorrência (non-compliance)
     */
    @Query("SELECT p FROM PontoEntrega p WHERE p.viagemId = :viagemId AND p.status = 'FALHOU' AND p.ocorrencia IS NULL")
    List<PontoEntrega> findFalhasSemOcorrencia(@Param("viagemId") Long viagemId);
}
