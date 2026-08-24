// =====================================================================
// MotoristaRepository.java
// =====================================================================
package com.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.Motorista;

import jakarta.persistence.LockModeType;

@Repository
public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    @Query(value = "SELECT * FROM motoristas WHERE cpf = :cpf LIMIT 1",
           nativeQuery = true)
    Optional<Motorista> findByCpf(@Param("cpf") String cpf);

    @Query(value = "SELECT * FROM motoristas WHERE cnh = :cnh LIMIT 1",
           nativeQuery = true)
    Optional<Motorista> findByCnh(@Param("cnh") String cnh);

    @Query(value = "SELECT * FROM motoristas WHERE email = :email LIMIT 1",
           nativeQuery = true)
    Optional<Motorista> findByEmail(@Param("email") String email);

    Optional<Motorista> findByCpfAndTenantId(String cpf, Long tenantId);

    boolean existsByCpfAndTenantId(String cpf, Long tenantId);

    boolean existsByCpfAndTenantIdAndIdNot(String cpf, Long tenantId, Long id);

    java.util.List<Motorista> findByTenantId(Long tenantId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Motorista m WHERE m.id = :id")
    Optional<Motorista> findByIdWithLock(@Param("id") Long id);
        
}
