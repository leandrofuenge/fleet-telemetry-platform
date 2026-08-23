package com.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.PareamentoDispositivo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface PareamentoDispositivoRepository extends JpaRepository<PareamentoDispositivo, Long> {
    boolean existsByCodigoHash(String codigoHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PareamentoDispositivo> findByCodigoHash(String codigoHash);
}
