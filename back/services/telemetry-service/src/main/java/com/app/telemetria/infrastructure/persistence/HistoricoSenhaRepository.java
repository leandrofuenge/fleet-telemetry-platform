package com.app.telemetria.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.HistoricoSenha;

@Repository
public interface HistoricoSenhaRepository extends JpaRepository<HistoricoSenha, Long> {

    List<HistoricoSenha> findTop5ByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    
    @Query("SELECT h.senhaHash FROM HistoricoSenha h WHERE h.usuarioId = :usuarioId ORDER BY h.criadoEm DESC")
    List<String> findUltimasSenhasHash(@Param("usuarioId") Long usuarioId);
    
    long countByUsuarioId(Long usuarioId);
}