// =====================================================================
// MotoristaRepository.java
// =====================================================================
package com.app.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.Motorista;

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
}