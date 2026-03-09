package com.app.telemetria.repository;

import com.app.telemetria.entity.GeocodingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GeocodingCacheRepository extends JpaRepository<GeocodingCache, Long> {
    
    /**
     * Busca coordenadas próximas com tolerância de ~100 metros
     * @param lat Latitude
     * @param lon Longitude
     * @return Cache mais recente dentro da tolerância
     */
    @Query("SELECT g FROM GeocodingCache g WHERE " +
           "ABS(g.latitude - :lat) < 0.001 AND " +  // ~100 metros de tolerância
           "ABS(g.longitude - :lon) < 0.001 " +
           "ORDER BY g.dataConsulta DESC")
    Optional<GeocodingCache> findProximo(@Param("lat") Double lat, @Param("lon") Double lon);
    
    /**
     * Encontra pelo par exato de coordenadas
     */
    Optional<GeocodingCache> findByLatitudeAndLongitude(Double latitude, Double longitude);
    
    /**
     * Deleta entradas antigas (para limpeza do cache)
     */
    void deleteByDataConsultaBefore(LocalDateTime data);
    
    /**
     * Conta entradas por período
     */
    long countByDataConsultaAfter(LocalDateTime data);
    
    /**
     * Busca por cidade (para cache pré-processado)
     */
    @Query("SELECT g FROM GeocodingCache g WHERE " +
           "g.cidade = :cidade AND g.pais = :pais")
    Optional<GeocodingCache> findByCidadeAndPais(@Param("cidade") String cidade, 
                                                  @Param("pais") String pais);
    
    /**
     * Busca o mais recente para uma coordenada aproximada
     */
    @Query("SELECT g FROM GeocodingCache g WHERE " +
           "g.latitude BETWEEN :latMin AND :latMax AND " +
           "g.longitude BETWEEN :lonMin AND :lonMax " +
           "ORDER BY g.dataConsulta DESC")
    Optional<GeocodingCache> findInBoundingBox(@Param("latMin") Double latMin,
                                               @Param("latMax") Double latMax,
                                               @Param("lonMin") Double lonMin,
                                               @Param("lonMax") Double lonMax);
    
    /**
     * Deleta entradas mais antigas que X dias
     */
    @Query("DELETE FROM GeocodingCache g WHERE g.dataConsulta < :data")
    void deleteOlderThan(@Param("data") LocalDateTime data);
    
    /**
     * Verifica se existe cache recente para uma região
     */
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM GeocodingCache g WHERE " +
           "g.latitude BETWEEN :latMin AND :latMax AND " +
           "g.longitude BETWEEN :lonMin AND :lonMax AND " +
           "g.dataConsulta > :dataRecente")
    boolean existsCacheRecenteNaRegiao(@Param("latMin") Double latMin,
                                       @Param("latMax") Double latMax,
                                       @Param("lonMin") Double lonMin,
                                       @Param("lonMax") Double lonMax,
                                       @Param("dataRecente") LocalDateTime dataRecente);
}