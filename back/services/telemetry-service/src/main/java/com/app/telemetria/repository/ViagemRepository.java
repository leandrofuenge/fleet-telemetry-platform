package com.app.telemetria.repository;

import com.app.telemetria.entity.Viagem;
import com.app.telemetria.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ViagemRepository extends JpaRepository<Viagem, Long> {
    
    List<Viagem> findByVeiculoOrderByDataSaidaDesc(Veiculo veiculo);
    
    List<Viagem> findByMotoristaIdOrderByDataSaidaDesc(Long motoristaId);
    
    List<Viagem> findByStatus(String status);
    
    @Query("SELECT v FROM Viagem v WHERE v.veiculo = :veiculo AND v.status = :status")
    Optional<Viagem> findByVeiculoAndStatus(@Param("veiculo") Veiculo veiculo, @Param("status") String status);
    
    @Query("SELECT v FROM Viagem v WHERE v.status = 'EM_ANDAMENTO'")
    List<Viagem> findAllEmAndamento();
    
    @Query("SELECT v FROM Viagem v WHERE v.dataChegadaPrevista < :agora AND v.status != 'FINALIZADA'")
    List<Viagem> findAtrasadas(@Param("agora") LocalDateTime agora);
    
    @Query("SELECT COUNT(v) FROM Viagem v WHERE v.status = :status")
    long countByStatus(@Param("status") String status);
    
    Optional<Viagem> findByVeiculoIdAndStatus(Long veiculoId, String status);
    
    // Outros métodos úteis
    Optional<Viagem> findByVeiculoIdAndStatusOrderByDataInicioDesc(Long veiculoId, String status);
    
    Optional<Viagem> findByMotoristaIdAndStatus(Long motoristaId, String status); 
    
}
    