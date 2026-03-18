package com.app.telemetria.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.telemetria.domain.entity.Veiculo;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    // Para warming de cache com Cliente e Motorista atual carregados
    @Query("select v from Veiculo v "
         + "left join fetch v.cliente "
         + "left join fetch v.motoristaAtual")
    List<Veiculo> findAllWithClienteAndMotorista();

    @Query(value = "SELECT * FROM veiculos WHERE placa = :placa LIMIT 1",
           nativeQuery = true)
    Optional<Veiculo> findByPlaca(@Param("placa") String placa);

    // Corrigido: removida a query nativa que retornava Long em vez de boolean.
    // O Spring Data JPA resolve automaticamente a partir do nome do método.
    boolean existsByPlaca(String placa);

    @Query(value = "SELECT * FROM veiculos WHERE modelo LIKE CONCAT('%', :modelo, '%')",
           nativeQuery = true)
    List<Veiculo> findByModeloContainingIgnoreCase(@Param("modelo") String modelo);

    @Query(value = "SELECT * FROM veiculos WHERE UPPER(marca) = UPPER(:marca)",
           nativeQuery = true)
    List<Veiculo> findByMarcaIgnoreCase(@Param("marca") String marca);

    @Query(value = "SELECT * FROM veiculos WHERE ativo = TRUE",
           nativeQuery = true)
    List<Veiculo> findByAtivoTrue();

    @Query(value = "SELECT * FROM veiculos WHERE cliente_id = :clienteId",
           nativeQuery = true)
    List<Veiculo> findByClienteId(@Param("clienteId") Long clienteId);

    @Query(value = "SELECT * FROM veiculos WHERE motorista_atual_id = :motoristaId",
           nativeQuery = true)
    List<Veiculo> findByMotoristaAtualId(@Param("motoristaId") Long motoristaId);

    @Query(value = "SELECT * FROM veiculos WHERE ano_fabricacao = :ano",
           nativeQuery = true)
    List<Veiculo> findByAnoFabricacao(@Param("ano") Integer ano);

    @Query(value = "SELECT * FROM veiculos WHERE capacidade_carga > :capacidade",
           nativeQuery = true)
    List<Veiculo> findByCapacidadeCargaGreaterThan(@Param("capacidade") Double capacidade);
}