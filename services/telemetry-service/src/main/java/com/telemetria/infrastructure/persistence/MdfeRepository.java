package com.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.Mdfe;

public interface MdfeRepository extends JpaRepository<Mdfe, Long> {
    Optional<Mdfe> findByViagemIdAndStatus(Long viagemId, String status);

    Optional<Mdfe> findByChaveMdfe(String chave);
}
