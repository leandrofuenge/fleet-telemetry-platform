package com.app.telemetria.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.app.telemetria.entity.Motorista;

public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    Optional<Motorista> findByCpf(String cpf);
    
    Optional<Motorista> findByCnh(String cnh);
    
    Optional<Motorista> findByEmail(String email); // NOVO MÉTODO

}
