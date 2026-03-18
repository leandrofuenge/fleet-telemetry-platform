// =====================================================================
// UsuarioRepository.java
// =====================================================================
package com.app.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query(value = "SELECT * FROM usuarios WHERE login = :login LIMIT 1",
           nativeQuery = true)
    Optional<Usuario> findByLogin(@Param("login") String login);
}
