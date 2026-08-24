package com.telemetria.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.MensagemViagem;

public interface MensagemViagemRepository extends JpaRepository<MensagemViagem, Long> {
    List<MensagemViagem> findByViagemIdOrderByCriadoEmAsc(Long viagemId);
}
