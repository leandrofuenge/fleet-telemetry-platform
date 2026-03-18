// =====================================================================
// GeocodingCacheRepository.java
// =====================================================================
package com.app.telemetria.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.app.telemetria.domain.entity.GeocodingCache;

@Repository
public interface GeocodingCacheRepository extends JpaRepository<GeocodingCache, Long> {

    @Query(value = "SELECT * FROM geocoding_cache WHERE lat_arred = :latArred AND lng_arred = :lngArred LIMIT 1",
           nativeQuery = true)
    Optional<GeocodingCache> findByLatArredAndLngArred(
            @Param("latArred") BigDecimal latArred,
            @Param("lngArred") BigDecimal lngArred);

    @Query(value = "SELECT * FROM geocoding_cache " +
                   "WHERE ABS(lat_arred - :lat) < 0.001 " +
                   "AND ABS(lng_arred - :lng) < 0.001 " +
                   "ORDER BY consulta_em DESC LIMIT 1",
           nativeQuery = true)
    Optional<GeocodingCache> findProximo(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM geocoding_cache WHERE expira_em < :data",
           nativeQuery = true)
    void deleteByExpiraEmBefore(@Param("data") LocalDateTime data);

    @Query(value = "SELECT COUNT(*) FROM geocoding_cache WHERE consulta_em > :data",
           nativeQuery = true)
    long countByConsultaEmAfter(@Param("data") LocalDateTime data);

    @Query(value = "SELECT * FROM geocoding_cache WHERE cidade = :cidade AND pais = :pais LIMIT 1",
           nativeQuery = true)
    Optional<GeocodingCache> findByCidadeAndPais(
            @Param("cidade") String cidade,
            @Param("pais") String pais);

    @Query(value = "SELECT * FROM geocoding_cache " +
                   "WHERE lat_arred BETWEEN :latMin AND :latMax " +
                   "AND lng_arred BETWEEN :lngMin AND :lngMax " +
                   "ORDER BY consulta_em DESC LIMIT 1",
           nativeQuery = true)
    Optional<GeocodingCache> findInBoundingBox(
            @Param("latMin") BigDecimal latMin,
            @Param("latMax") BigDecimal latMax,
            @Param("lngMin") BigDecimal lngMin,
            @Param("lngMax") BigDecimal lngMax);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM geocoding_cache WHERE expira_em < :data",
           nativeQuery = true)
    void deleteExpiredBefore(@Param("data") LocalDateTime data);

    @Query(value = "SELECT COUNT(*) > 0 FROM geocoding_cache " +
                   "WHERE lat_arred BETWEEN :latMin AND :latMax " +
                   "AND lng_arred BETWEEN :lngMin AND :lngMax " +
                   "AND consulta_em > :dataRecente",
           nativeQuery = true)
    boolean existsCacheRecenteNaRegiao(
            @Param("latMin") BigDecimal latMin,
            @Param("latMax") BigDecimal latMax,
            @Param("lngMin") BigDecimal lngMin,
            @Param("lngMax") BigDecimal lngMax,
            @Param("dataRecente") LocalDateTime dataRecente);
}