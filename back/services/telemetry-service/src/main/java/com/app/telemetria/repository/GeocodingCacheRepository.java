package com.app.telemetria.repository;

import com.app.telemetria.entity.GeocodingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GeocodingCacheRepository extends JpaRepository<GeocodingCache, Long> {

       /**
        * Busca pelo par exato de coordenadas arredondadas (respeita a unique
        * constraint).
        */
       Optional<GeocodingCache> findByLatArredAndLngArred(BigDecimal latArred, BigDecimal lngArred);

       /**
        * Busca coordenadas próximas com tolerância de ~100 metros,
        * considerando que latArred/lngArred têm precisão de 4 casas decimais (~11m).
        */
       @Query("SELECT g FROM GeocodingCache g WHERE " +
                     "ABS(g.latArred - :lat) < 0.001 AND " + // 0.001 graus ≈ 100m (aproximação)
                     "ABS(g.lngArred - :lng) < 0.001 " +
                     "ORDER BY g.consultaEm DESC")
       Optional<GeocodingCache> findProximo(@Param("lat") BigDecimal lat, @Param("lng") BigDecimal lng);

       /**
        * Deleta entradas cuja data de expiração seja anterior à data informada.
        */
       void deleteByExpiraEmBefore(LocalDateTime data);

       /**
        * Conta entradas criadas após uma determinada data.
        */
       long countByConsultaEmAfter(LocalDateTime data);

       /**
        * Busca por cidade e país (para cache pré-processado).
        */
       @Query("SELECT g FROM GeocodingCache g WHERE " +
                     "g.cidade = :cidade AND g.pais = :pais")
       Optional<GeocodingCache> findByCidadeAndPais(@Param("cidade") String cidade,
                     @Param("pais") String pais);

       /**
        * Busca o mais recente dentro de uma bounding box definida pelos limites das
        * coordenadas arredondadas.
        */
       @Query("SELECT g FROM GeocodingCache g WHERE " +
                     "g.latArred BETWEEN :latMin AND :latMax AND " +
                     "g.lngArred BETWEEN :lngMin AND :lngMax " +
                     "ORDER BY g.consultaEm DESC")
       Optional<GeocodingCache> findInBoundingBox(@Param("latMin") BigDecimal latMin,
                     @Param("latMax") BigDecimal latMax,
                     @Param("lngMin") BigDecimal lngMin,
                     @Param("lngMax") BigDecimal lngMax);

       /**
        * Deleta entradas expiradas (cuja data de expiração seja anterior à data
        * informada).
        */
       @Query("DELETE FROM GeocodingCache g WHERE g.expiraEm < :data")
       void deleteExpiredBefore(@Param("data") LocalDateTime data);

       /**
        * Verifica se existe cache recente (consultaEm > dataRecente) dentro de uma
        * bounding box.
        */
       @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM GeocodingCache g WHERE " +
                     "g.latArred BETWEEN :latMin AND :latMax AND " +
                     "g.lngArred BETWEEN :lngMin AND :lngMax AND " +
                     "g.consultaEm > :dataRecente")
       boolean existsCacheRecenteNaRegiao(@Param("latMin") BigDecimal latMin,
                     @Param("latMax") BigDecimal latMax,
                     @Param("lngMin") BigDecimal lngMin,
                     @Param("lngMax") BigDecimal lngMax,
                     @Param("dataRecente") LocalDateTime dataRecente);
}