package com.telemetria.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Telemetria;

/**
 * Repositório para a entidade Telemetria
 * Responsável por operações de banco de dados relacionadas a dados de telemetria
 * 
 * @author Telemetria Team
 * @version 1.0
 */
@Repository
public interface TelemetriaRepository extends JpaRepository<Telemetria, Long> {

    Logger log = LoggerFactory.getLogger(TelemetriaRepository.class);

    // =========================================
    // SAVE com LOG - Método customizado
    // =========================================

    /**
     * Salva uma telemetria no banco de dados com logging detalhado
     * Útil para auditoria e debug do processo de persistência
     * 
     * @param telemetria Entidade Telemetria a ser salva
     * @return Telemetria salva com ID gerado
     */
    default Telemetria saveWithLog(Telemetria telemetria) {
        log.info("💾 [REPOSITORY] Salvando telemetria no banco...");
        log.debug("[REPOSITORY] Dados - ID: {}, VeiculoID: {}, DataHora: {}, Lat: {}, Lng: {}",
                telemetria.getId(),
                telemetria.getVeiculoId(),
                telemetria.getDataHora(),
                telemetria.getLatitude(),
                telemetria.getLongitude());
        
        Telemetria saved = save(telemetria);
        
        log.info("✅ [REPOSITORY] Telemetria salva! ID gerado: {}", saved.getId());
        return saved;
    }

    // =========================================
    // MÉTODOS POR VEÍCULO (Spring Data JPA)
    // =========================================

    /**
     * Busca todas as telemetrias de um veículo ordenadas por data/hora decrescente
     * Utiliza método derivado do Spring Data JPA
     * 
     * @param veiculoId Identificador do veículo
     * @return Lista de telemetrias do veículo (mais recentes primeiro)
     */
    List<Telemetria> findByVeiculoIdOrderByDataHoraDesc(Long veiculoId);

    /**
     * Busca todas as telemetrias de um veículo ordenadas por data/hora crescente
     * 
     * @param veiculoId Identificador do veículo
     * @return Lista de telemetrias do veículo (mais antigas primeiro)
     */
    List<Telemetria> findByVeiculoIdOrderByDataHoraAsc(Long veiculoId);

    // =========================================
    // Métodos por ID do veículo (SQL Nativo)
    // =========================================

    /**
     * Busca todas as telemetrias de um veículo ordenadas por data/hora decrescente
     * Utiliza SQL nativo para performance otimizada
     * 
     * @param veiculoId Identificador do veículo
     * @return Lista de telemetrias do veículo (mais recentes primeiro)
     */
    @Query(value = "SELECT * FROM telemetria WHERE veiculo_id = :veiculoId ORDER BY data_hora DESC",
           nativeQuery = true)
    List<Telemetria> findByVeiculoOrderByDataHoraDesc(@Param("veiculoId") Long veiculoId);

    /**
     * Busca a última telemetria (mais recente) de um veículo
     * 
     * @param veiculoId Identificador do veículo
     * @return Optional contendo a última telemetria, se existir
     */
    @Query(value = "SELECT * FROM telemetria WHERE veiculo_id = :veiculoId ORDER BY data_hora DESC LIMIT 1",
           nativeQuery = true)
    Optional<Telemetria> findUltimaTelemetriaByVeiculo(@Param("veiculoId") Long veiculoId);

    /**
     * Busca telemetrias de um veículo em um intervalo de tempo específico
     * Ordenadas por data/hora crescente para análise sequencial
     * 
     * @param veiculoId Identificador do veículo
     * @param inicio Data/hora inicial do período
     * @param fim Data/hora final do período
     * @return Lista de telemetrias do período (ordem cronológica)
     */
    @Query(value = "SELECT * FROM telemetria " +
                   "WHERE veiculo_id = :veiculoId " +
                   "AND data_hora BETWEEN :inicio AND :fim " +
                   "ORDER BY data_hora ASC",
           nativeQuery = true)
    List<Telemetria> findByVeiculoAndDataHoraBetweenOrderByDataHoraAsc(
            @Param("veiculoId") Long veiculoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    /**
     * Busca telemetrias recentes de um veículo a partir de uma data específica
     * 
     * @param veiculoId Identificador do veículo
     * @param data Data de referência (busca registros a partir desta data)
     * @return Lista de telemetrias recentes (mais recentes primeiro)
     */
    @Query(value = "SELECT * FROM telemetria " +
                   "WHERE veiculo_id = :veiculoId AND data_hora >= :data " +
                   "ORDER BY data_hora DESC",
           nativeQuery = true)
    List<Telemetria> findRecentByVeiculo(
            @Param("veiculoId") Long veiculoId,
            @Param("data") LocalDateTime data);

    /**
     * Conta o número total de telemetrias de um veículo
     * 
     * @param veiculoId Identificador do veículo
     * @return Quantidade de telemetrias do veículo
     */
    @Query(value = "SELECT COUNT(*) FROM telemetria WHERE veiculo_id = :veiculoId",
           nativeQuery = true)
    long countByVeiculo(@Param("veiculoId") Long veiculoId);

    /**
     * Deleta telemetrias mais antigas que uma data específica
     * Utilizado para políticas de retenção de dados
     * 
     * @param data Data limite (deleta registros com data_hora anterior a esta)
     */
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM telemetria WHERE data_hora < :data",
           nativeQuery = true)
    void deleteByDataHoraBefore(@Param("data") LocalDateTime data);

    // =========================================
    // Métodos de Busca por ID (Versão JPA)
    // =========================================

    /**
     * Busca a última telemetria de um veículo (top 1 por data/hora)
     * Utiliza método derivado do Spring Data JPA
     * 
     * @param veiculoId Identificador do veículo
     * @return Optional com a última telemetria
     */
    Optional<Telemetria> findTopByVeiculoIdOrderByDataHoraDesc(Long veiculoId);

    /**
     * Busca a última telemetria de um veículo pelo ID (alias)
     * Este método é um alias para findTopByVeiculoIdOrderByDataHoraDesc
     * 
     * @param veiculoId Identificador do veículo
     * @return Optional contendo a última telemetria, se existir
     */
    default Optional<Telemetria> findUltimaTelemetriaByVeiculoId(Long veiculoId) {
        log.debug("🔍 Buscando última telemetria para veículo ID: {}", veiculoId);
        return findTopByVeiculoIdOrderByDataHoraDesc(veiculoId);
    }

    /**
     * Busca a última telemetria de um veículo com um dispositivo específico
     * 
     * @param veiculoId Identificador do veículo
     * @param deviceId Identificador do dispositivo IoT
     * @return Optional contendo a última telemetria do dispositivo
     */
    Optional<Telemetria> findTopByVeiculoIdAndDeviceIdOrderByDataHoraDesc(Long veiculoId, String deviceId);

    /**
     * Busca a última telemetria anterior a uma data específica
     * Utilizado para análise de histórico
     * 
     * @param veiculoId Identificador do veículo
     * @param dataHora Data/hora de referência
     * @return Optional contendo a última telemetria antes da data
     */
    @Query(value = "SELECT * FROM telemetria " +
                   "WHERE veiculo_id = :veiculoId " +
                   "AND data_hora < :dataHora " +
                   "ORDER BY data_hora DESC LIMIT 1",
           nativeQuery = true)
    Optional<Telemetria> findUltimaTelemetriaAntes(@Param("veiculoId") Long veiculoId, 
                                                  @Param("dataHora") LocalDateTime dataHora);

    /**
     * Busca telemetrias por período de tempo, ordenadas por data/hora decrescente
     * 
     * @param inicio Data/hora inicial do período
     * @param fim Data/hora final do período
     * @return Lista de telemetrias no período (mais recentes primeiro)
     */
    List<Telemetria> findByDataHoraBetweenOrderByDataHoraDesc(LocalDateTime inicio, LocalDateTime fim);

    /**
     * Busca telemetrias de um veículo em um período, ordenadas cronologicamente
     * 
     * @param veiculoId Identificador do veículo
     * @param inicio Data/hora inicial
     * @param fim Data/hora final
     * @return Lista de telemetrias em ordem cronológica
     */
    List<Telemetria> findByVeiculoIdAndDataHoraBetweenOrderByDataHoraAsc(Long veiculoId, LocalDateTime inicio, LocalDateTime fim);

    // =========================================
    // MÉTODOS ADICIONADOS: findByVeiculoIdAndDataHoraBetween
    // =========================================

    /**
     * Busca telemetrias de um veículo em um intervalo de tempo específico
     * Utiliza método derivado do Spring Data JPA
     * 
     * @param veiculoId Identificador do veículo
     * @param inicio Data/hora inicial do período
     * @param fim Data/hora final do período
     * @return Lista de telemetrias do veículo no período
     */
    List<Telemetria> findByVeiculoIdAndDataHoraBetween(Long veiculoId, LocalDateTime inicio, LocalDateTime fim);

    /**
     * Busca telemetrias de um veículo em um intervalo de tempo, ordenadas por data/hora decrescente
     * 
     * @param veiculoId Identificador do veículo
     * @param inicio Data/hora inicial do período
     * @param fim Data/hora final do período
     * @return Lista de telemetrias do veículo no período (mais recentes primeiro)
     */
    List<Telemetria> findByVeiculoIdAndDataHoraBetweenOrderByDataHoraDesc(Long veiculoId, LocalDateTime inicio, LocalDateTime fim);

    // =========================================
    // RN-VIA-002: Métodos para Score da Viagem
    // =========================================

    /**
     * RN-VIA-002: Busca telemetrias de uma viagem ordenadas por data/hora crescente
     * Necessário para calcular frenagens bruscas e excessos de velocidade
     * 
     * @param viagemId Identificador da viagem
     * @return Lista de telemetrias da viagem em ordem cronológica
     */
    List<Telemetria> findByViagemIdOrderByDataHoraAsc(Long viagemId);

    /**
     * RN-VIA-002: Busca telemetrias de uma viagem ordenadas por data/hora decrescente
     * 
     * @param viagemId Identificador da viagem
     * @return Lista de telemetrias da viagem (mais recentes primeiro)
     */
    List<Telemetria> findByViagemIdOrderByDataHoraDesc(Long viagemId);

    /**
     * Busca todas as telemetrias associadas a uma viagem
     * 
     * @param viagemId Identificador da viagem
     * @return Lista de telemetrias da viagem
     */
    List<Telemetria> findByViagemId(Long viagemId);

    /**
     * Busca telemetrias de um veículo com suporte a paginação
     * 
     * @param veiculoId Identificador do veículo
     * @param pageable Configurações de paginação
     * @return Página de telemetrias
     */
    Page<Telemetria> findByVeiculoId(Long veiculoId, Pageable pageable);

    /**
     * Conta o número de telemetrias de um veículo em um período específico
     * 
     * @param veiculoId Identificador do veículo
     * @param inicio Data/hora inicial
     * @param fim Data/hora final
     * @return Quantidade de telemetrias no período
     */
    long countByVeiculoIdAndDataHoraBetween(Long veiculoId, LocalDateTime inicio, LocalDateTime fim);

    /**
     * Busca telemetrias com velocidade acima do limite em um período
     * Utilizado para identificar infrações de velocidade
     * 
     * @param veiculoId Identificador do veículo
     * @param limiteVelocidade Velocidade máxima permitida (km/h)
     * @param inicio Data/hora inicial
     * @param fim Data/hora final
     * @return Lista de telemetrias com excesso de velocidade
     */
    @Query("SELECT t FROM Telemetria t WHERE t.veiculoId = :veiculoId AND t.velocidade > :limiteVelocidade AND t.dataHora BETWEEN :inicio AND :fim")
    List<Telemetria> findExcessosVelocidadeByVeiculo(
            @Param("veiculoId") Long veiculoId,
            @Param("limiteVelocidade") Double limiteVelocidade,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    /**
     * RN-VIA-002: Busca telemetrias com frenagem brusca (redução abrupta de velocidade)
     * Identifica reduções de velocidade superiores ao limite em um único intervalo
     * 
     * @param veiculoId Identificador do veículo
     * @param reducaoMinima Redução mínima de velocidade para considerar frenagem brusca (km/h)
     * @param inicio Data/hora inicial do período
     * @param fim Data/hora final do período
     * @return Lista de telemetrias onde ocorreu frenagem brusca
     */
    @Query(value = """
        SELECT t1.* FROM telemetria t1
        INNER JOIN telemetria t2 ON t2.id = (
            SELECT id FROM telemetria t3 
            WHERE t3.veiculo_id = t1.veiculo_id 
            AND t3.data_hora < t1.data_hora 
            ORDER BY t3.data_hora DESC LIMIT 1
        )
        WHERE t1.veiculo_id = :veiculoId 
        AND (t2.velocidade - t1.velocidade) > :reducaoMinima
        AND t1.data_hora BETWEEN :inicio AND :fim
    """, nativeQuery = true)
    List<Telemetria> findFrenagensBruscasByVeiculo(
            @Param("veiculoId") Long veiculoId,
            @Param("reducaoMinima") Double reducaoMinima,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // =========================================
    // RF06 - Monitoramento de Posição
    // =========================================

    /**
     * RF06: Busca veículos que estão sem sinal GPS há mais de X minutos e com ignição ligada
     * Utilizado para monitoramento de veículos "fantasma"
     * 
     * @param minutosSemSinal Número mínimo de minutos sem receber sinal
     * @param ignicaoOn Status da ignição (true = ligada)
     * @return Lista de IDs dos veículos sem sinal
     */
    @Query(value = """
        SELECT DISTINCT t.veiculo_id 
        FROM telemetria t 
        JOIN posicao_atual pa ON t.veiculo_id = pa.veiculo_id 
        WHERE TIMESTAMPDIFF(MINUTE, t.data_hora, NOW()) > :minutosSemSinal
          AND pa.ignicao = :ignicaoOn
          AND pa.status_veiculo != 'DESCONHECIDO'
        """, nativeQuery = true)
    List<Long> findVeiculosSemSinal(@Param("minutosSemSinal") int minutosSemSinal, 
                                   @Param("ignicaoOn") Boolean ignicaoOn);

    /**
     * RF06: Atualiza status de veículos para DESCONHECIDO após 5 minutos sem telemetria
     * 
     * @return Número de registros atualizados
     */
    @Modifying
    @Query(value = """
        UPDATE posicao_atual 
        SET status_veiculo = 'DESCONHECIDO' 
        WHERE TIMESTAMPDIFF(MINUTE, ultima_telemetria, NOW()) > 5
        """, nativeQuery = true)
    int atualizarStatusDesconhecido();

    /**
     * Atualiza status de veículos para DESCONHECIDO após X minutos sem telemetria
     * 
     * @param minutos Número de minutos sem telemetria para considerar desconhecido
     * @return Número de registros atualizados
     */
    @Modifying
    @Query(value = """
        UPDATE posicao_atual 
        SET status_veiculo = 'DESCONHECIDO' 
        WHERE TIMESTAMPDIFF(MINUTE, ultima_telemetria, NOW()) > :minutos
        """, nativeQuery = true)
    int atualizarStatusDesconhecido(@Param("minutos") int minutos);

    // =========================================
    // RN-POS-002: Retenção de Dados
    // =========================================

    /**
     * Deleta telemetrias antigas de um veículo que não são de jornada nem preservadas
     * 
     * @param veiculoId Identificador do veículo
     * @param dataLimite Data limite para deleção
     * @return Número de registros deletados
     */
    @Modifying
    @Query(value = """
        DELETE FROM telemetria 
        WHERE veiculo_id = :veiculoId 
          AND data_hora < :dataLimite 
          AND (preservar_dados IS NULL OR preservar_dados = FALSE)
          AND tipo NOT IN ('JORNADA_LEI_12619')
        """, nativeQuery = true)
    int deleteByVeiculoIdAndDataHoraBeforeAndPreservarDadosFalse(
        @Param("veiculoId") Long veiculoId, 
        @Param("dataLimite") LocalDateTime dataLimite);

    /**
     * RN-POS-002: Deleta telemetrias normais (não jornada, não preservadas) 
     * com data anterior ao limite estabelecido
     * 
     * @param veiculoId Identificador do veículo
     * @param dataLimite Data limite para deleção
     * @return Número de registros deletados
     */
    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM telemetria 
        WHERE veiculo_id = :veiculoId 
          AND data_hora < :dataLimite 
          AND (preservar_dados IS NULL OR preservar_dados = FALSE)
          AND (tipo IS NULL OR tipo NOT IN ('JORNADA_LEI_12619'))
        """, nativeQuery = true)
    int deleteDadosNormaisAntigos(@Param("veiculoId") Long veiculoId, 
                                 @Param("dataLimite") LocalDateTime dataLimite);

    /**
     * RN-POS-002: Deleta dados de jornada (Lei 12.619) com data anterior ao limite (2 anos),
     * desde que não estejam marcados como preservar_dados
     * 
     * @param veiculoId Identificador do veículo
     * @param dataLimite Data limite para deleção (normalmente 2 anos atrás)
     * @return Número de registros deletados
     */
    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM telemetria 
        WHERE veiculo_id = :veiculoId 
          AND data_hora < :dataLimite 
          AND (preservar_dados IS NULL OR preservar_dados = FALSE)
          AND tipo = 'JORNADA_LEI_12619'
        """, nativeQuery = true)
    int deleteJornadaAntiga(@Param("veiculoId") Long veiculoId, 
                          @Param("dataLimite") LocalDateTime dataLimite);
}