package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telemetria.domain.entity.DesvioRota;

/**
 * Repositório para a entidade DesvioRota
 * Responsável por operações de banco de dados relacionadas a desvios de rota
 * 
 * @author Telemetria Team
 * @version 1.0
 */
@Repository
public interface DesvioRotaRepository extends JpaRepository<DesvioRota, Long> {

    // ================ MÉTODOS COM SQL NATIVO ================

    /**
     * Busca todos os desvios de uma rota específica, ordenados do mais recente para o mais antigo
     * 
     * @param rotaId Identificador da rota
     * @return Lista de desvios da rota ordenados por data/hora decrescente
     */
    @Query(value = "SELECT * FROM desvios_rota WHERE rota_id = :rotaId ORDER BY data_hora_desvio DESC",
           nativeQuery = true)
    List<DesvioRota> findByRotaIdOrderByDataHoraDesvioDesc(@Param("rotaId") Long rotaId);

    /**
     * Busca todos os desvios de um veículo específico, ordenados do mais recente para o mais antigo
     * 
     * @param veiculoId Identificador do veículo
     * @return Lista de desvios do veículo ordenados por data/hora decrescente
     */
    @Query(value = "SELECT * FROM desvios_rota WHERE veiculo_id = :veiculoId ORDER BY data_hora_desvio DESC",
           nativeQuery = true)
    List<DesvioRota> findByVeiculoIdOrderByDataHoraDesvioDesc(@Param("veiculoId") Long veiculoId);

    /**
     * Busca todos os desvios que ainda não foram resolvidos
     * 
     * @return Lista de desvios ativos (não resolvidos)
     */
    @Query(value = "SELECT * FROM desvios_rota WHERE resolvido = FALSE",
           nativeQuery = true)
    List<DesvioRota> findByResolvidoFalse();

    /**
     * Busca todos os desvios que já foram resolvidos
     * 
     * @return Lista de desvios resolvidos
     */
    @Query(value = "SELECT * FROM desvios_rota WHERE resolvido = TRUE",
           nativeQuery = true)
    List<DesvioRota> findByResolvidoTrue();

    /**
     * Busca um desvio ativo (não resolvido) para uma rota específica
     * Retorna apenas o primeiro desvio encontrado (LIMIT 1)
     * 
     * @param rotaId Identificador da rota
     * @return Optional contendo o desvio ativo, se existir
     */
    @Query(value = "SELECT * FROM desvios_rota WHERE rota_id = :rotaId AND resolvido = FALSE LIMIT 1",
           nativeQuery = true)
    Optional<DesvioRota> findByRotaIdAndResolvidoFalse(@Param("rotaId") Long rotaId);

    /**
     * Busca um desvio ativo (não resolvido) para um veículo específico
     * Retorna apenas o primeiro desvio encontrado (LIMIT 1)
     * 
     * @param veiculoId Identificador do veículo
     * @return Optional contendo o desvio ativo, se existir
     */
    @Query(value = "SELECT * FROM desvios_rota WHERE veiculo_id = :veiculoId AND resolvido = FALSE LIMIT 1",
           nativeQuery = true)
    Optional<DesvioRota> findByVeiculoIdAndResolvidoFalse(@Param("veiculoId") Long veiculoId);

    // ================ MÉTODOS DE CONSULTA ESPECÍFICOS ================

    /**
     * Busca todos os desvios ativos (não resolvidos) para uma rota específica
     * Ordenados do mais recente para o mais antigo
     * 
     * @param rotaId Identificador da rota
     * @return Lista de desvios ativos da rota
     */
    @Query(value = "SELECT * FROM desvios_rota WHERE rota_id = :rotaId AND resolvido = FALSE ORDER BY data_hora_desvio DESC",
           nativeQuery = true)
    List<DesvioRota> findDesviosAtivosPorRota(@Param("rotaId") Long rotaId);

    // ================ MÉTODOS DE CONTAGEM COM SQL NATIVO ================

    /**
     * Conta a quantidade de desvios ativos (não resolvidos) para um veículo específico
     * 
     * @param veiculoId Identificador do veículo
     * @return Número total de desvios ativos do veículo
     */
    @Query(value = "SELECT COUNT(*) FROM desvios_rota WHERE veiculo_id = :veiculoId AND resolvido = FALSE",
           nativeQuery = true)
    long countDesviosAtivosPorVeiculo(@Param("veiculoId") Long veiculoId);

    // ================ MÉTODOS DERIVADOS DO JPA ================

    /**
     * Busca todos os desvios de uma rota específica
     * Utiliza o método derivado do Spring Data JPA
     * 
     * @param rotaId Identificador da rota
     * @return Lista de desvios da rota
     */
    List<DesvioRota> findByRotaId(Long rotaId);
    
    /**
     * Busca todos os desvios de um veículo específico
     * Utiliza o método derivado do Spring Data JPA
     * 
     * @param veiculoId Identificador do veículo
     * @return Lista de desvios do veículo
     */
    List<DesvioRota> findByVeiculoId(Long veiculoId);
    
    /**
     * RN-VIA-002: Busca todos os desvios associados a uma viagem específica
     * Necessário para calcular penalidades de desvio no score da viagem
     * 
     * @param viagemId Identificador da viagem
     * @return Lista de desvios ocorridos durante a viagem
     */
    List<DesvioRota> findByViagemId(Long viagemId);
    
    /**
     * RN-VIA-002: Busca desvios de uma viagem filtrando pelo status de resolução
     * 
     * @param viagemId Identificador da viagem
     * @param resolvido Status de resolução (true=resolvido, false=não resolvido)
     * @return Lista de desvios da viagem com o status especificado
     */
    List<DesvioRota> findByViagemIdAndResolvido(Long viagemId, Boolean resolvido);
    
    /**
     * RN-VIA-002: Busca desvios ativos (não resolvidos) de uma viagem específica
     * Utiliza JPQL para garantir que apenas desvios não resolvidos sejam retornados
     * 
     * @param viagemId Identificador da viagem
     * @return Lista de desvios ativos da viagem
     */
    @Query("SELECT d FROM DesvioRota d WHERE d.viagemId = :viagemId AND d.resolvido = false")
    List<DesvioRota> findDesviosAtivosByViagemId(@Param("viagemId") Long viagemId);
    
    /**
     * Busca desvios ocorridos em um período específico
     * 
     * @param inicio Data/hora inicial do período (inclusive)
     * @param fim Data/hora final do período (inclusive)
     * @return Lista de desvios ocorridos no período
     */
    List<DesvioRota> findByDataHoraDesvioBetween(LocalDateTime inicio, LocalDateTime fim);
    
    /**
     * Conta o número total de desvios associados a uma viagem
     * 
     * @param viagemId Identificador da viagem
     * @return Quantidade de desvios da viagem
     */
    long countByViagemId(Long viagemId);
    
    /**
     * Busca desvios de um veículo com suporte a paginação
     * 
     * @param veiculoId Identificador do veículo
     * @param pageable Configurações de paginação (página, tamanho, ordenação)
     * @return Página de desvios do veículo
     */
    Page<DesvioRota> findByVeiculoId(Long veiculoId, Pageable pageable);
    
    /**
     * Busca desvios de uma rota com suporte a paginação
     * 
     * @param rotaId Identificador da rota
     * @param pageable Configurações de paginação (página, tamanho, ordenação)
     * @return Página de desvios da rota
     */
    Page<DesvioRota> findByRotaId(Long rotaId, Pageable pageable);
    
    /**
     * RN-ROT-002: Busca desvios que acumularam km extras acima do limite especificado
     * Utilizado para identificar desvios que necessitam de alerta crítico
     * 
     * @param limiteKm Limite de quilômetros extras (padrão: 2.0 km)
     * @return Lista de desvios com km extras acima do limite e ainda não resolvidos
     */
    @Query("SELECT d FROM DesvioRota d WHERE d.kmExtras > :limiteKm AND d.resolvido = false")
    List<DesvioRota> findDesviosComKmExtrasAcima(@Param("limiteKm") Double limiteKm);
    
    // ================ RN-DEV-002: MÉTODOS DE APROVAÇÃO DO GESTOR ================
    
    /**
     * RN-DEV-002: Busca desvios pendentes de aprovação do gestor há mais de 24 horas
     * Estes desvios devem reaparecer no painel de pendências
     * 
     * @param dataLimite Data/hora limite (24h atrás)
     * @return Lista de desvios pendentes há mais de 24h
     */
    @Query("SELECT d FROM DesvioRota d WHERE d.aprovadoGestor IS NULL AND d.criadoEm < :dataLimite ORDER BY d.criadoEm ASC")
    List<DesvioRota> findDesviosPendentesMais24h(@Param("dataLimite") LocalDateTime dataLimite);
    
    /**
     * RN-DEV-002: Busca todos os desvios pendentes de aprovação (aprovado_gestor IS NULL)
     * 
     * @return Lista de desvios aguardando aprovação do gestor
     */
    @Query("SELECT d FROM DesvioRota d WHERE d.aprovadoGestor IS NULL ORDER BY d.criadoEm DESC")
    List<DesvioRota> findDesviosPendentesAprovacao();
    
    /**
     * RN-DEV-002: Busca desvios pendentes de aprovação para um veículo específico
     * 
     * @param veiculoId Identificador do veículo
     * @return Lista de desvios pendentes do veículo
     */
    @Query("SELECT d FROM DesvioRota d WHERE d.veiculoId = :veiculoId AND d.aprovadoGestor IS NULL ORDER BY d.criadoEm DESC")
    List<DesvioRota> findDesviosPendentesByVeiculoId(@Param("veiculoId") Long veiculoId);
    
    /**
     * RN-DEV-002: Busca desvios reprovados por um gestor específico
     * 
     * @param gestorId Identificador do gestor
     * @return Lista de desvios reprovados pelo gestor
     */
    @Query("SELECT d FROM DesvioRota d WHERE d.gestorId = :gestorId AND d.aprovadoGestor = false ORDER BY d.dataAprovacaoGestor DESC")
    List<DesvioRota> findDesviosReprovadosByGestorId(@Param("gestorId") Long gestorId);
    
    /**
     * RN-DEV-002: Conta desvios por status de aprovação
     * 
     * @param aprovadoGestor Status de aprovação (NULL = pendente, TRUE = aprovado, FALSE = reprovado)
     * @return Quantidade de desvios com o status especificado
     */
    @Query("SELECT COUNT(d) FROM DesvioRota d WHERE d.aprovadoGestor = :aprovadoGestor")
    long countByAprovadoGestor(@Param("aprovadoGestor") Boolean aprovadoGestor);
    
    /**
     * RN-DEV-002: Busca desvios aprovados por um gestor específico
     * 
     * @param gestorId Identificador do gestor
     * @return Lista de desvios aprovados pelo gestor
     */
    @Query("SELECT d FROM DesvioRota d WHERE d.gestorId = :gestorId AND d.aprovadoGestor = true ORDER BY d.dataAprovacaoGestor DESC")
    List<DesvioRota> findDesviosAprovadosByGestorId(@Param("gestorId") Long gestorId);
}