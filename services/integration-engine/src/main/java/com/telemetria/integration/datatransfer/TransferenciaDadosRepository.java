package com.telemetria.integration.datatransfer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferenciaDadosRepository extends JpaRepository<TransferenciaDados, UUID> {
}
