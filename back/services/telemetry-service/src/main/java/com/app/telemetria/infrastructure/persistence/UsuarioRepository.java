package com.app.telemetria.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);
    
    Optional<Usuario> findByEmail(String email);
    
    Optional<Usuario> findByCpf(String cpf);
    
    boolean existsByLogin(String login);
    
    boolean existsByEmail(String email);
    
    boolean existsByCpf(String cpf);
    
    @Query("SELECT u FROM Usuario u WHERE u.login = :login AND u.ativo = true")
    Optional<Usuario> findAtivoByLogin(@Param("login") String login);
}