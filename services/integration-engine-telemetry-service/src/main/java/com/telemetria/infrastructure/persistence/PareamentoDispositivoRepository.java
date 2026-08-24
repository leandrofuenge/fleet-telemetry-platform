package com.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.telemetria.domain.entity.PareamentoDispositivo;

import jakarta.persistence.LockModeType;

public interface PareamentoDispositivoRepository extends JpaRepository<PareamentoDispositivo, Long> {
    boolean existsByCodigoHash(String codigoHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PareamentoDispositivo> findByCodigoHash(String codigoHash);
}
