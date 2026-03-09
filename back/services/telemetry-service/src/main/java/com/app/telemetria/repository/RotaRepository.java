package com.app.telemetria.repository;

import com.app.telemetria.entity.Rota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RotaRepository extends JpaRepository<Rota, Long> {
    
    // Buscar rotas por status
    List<Rota> findByStatus(String status);
    
    // Buscar rotas ativas de um veículo
    List<Rota> findByVeiculoIdAndStatus(Long veiculoId, String status);
    
    // Buscar rotas por período
    List<Rota> findByDataInicioBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
}