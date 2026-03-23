package com.app.telemetria.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.app.telemetria.domain.entity.SessaoAtiva;

@Repository
public interface SessaoAtivaRepository extends JpaRepository<SessaoAtiva, Long> {

    List<SessaoAtiva> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);
    
    Optional<SessaoAtiva> findByTokenJwt(String tokenJwt);
    
    long countByUsuarioId(Long usuarioId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM SessaoAtiva s WHERE s.usuarioId = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM SessaoAtiva s WHERE s.dataExpiracao < CURRENT_TIMESTAMP")
    int deleteAllExpiradas(); // ALTERADO: int em vez de void
    
    @Query("SELECT s FROM SessaoAtiva s WHERE s.usuarioId = :usuarioId AND s.dataExpiracao > CURRENT_TIMESTAMP")
    List<SessaoAtiva> findSessoesAtivasByUsuarioId(@Param("usuarioId") Long usuarioId);
}