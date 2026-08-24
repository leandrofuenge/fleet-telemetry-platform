package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.TelemetriaOutboxEvent;

import jakarta.persistence.LockModeType;

@Repository
public interface TelemetriaOutboxRepository extends JpaRepository<TelemetriaOutboxEvent, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM TelemetriaOutboxEvent e
            WHERE e.status = 'PENDENTE' AND e.proximaTentativaEm <= :agora
            ORDER BY e.criadoEm
            """)
    List<TelemetriaOutboxEvent> findPending(@Param("agora") LocalDateTime agora, Pageable pageable);
}
