package com.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.telemetria.domain.entity.ApiClient;

public interface ApiClientRepository extends JpaRepository<ApiClient, Long> {
    Optional<ApiClient> findByClientIdAndAtivoTrue(String clientId);
}
