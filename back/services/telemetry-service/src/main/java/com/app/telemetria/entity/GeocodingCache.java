package com.app.telemetria.entity;

// ─────────────────────────────────────────────────────────────
// 7. GeocodingCache.java
// ─────────────────────────────────────────────────────────────

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "geocoding_cache", uniqueConstraints = @UniqueConstraint(name = "uk_geocoding_coords", columnNames = {
        "lat_arred", "lng_arred" }), indexes = @Index(name = "idx_geo_expira", columnList = "expira_em"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeocodingCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lat_arred", nullable = false, precision = 7, scale = 4)
    private BigDecimal latArred;

    @Column(name = "lng_arred", nullable = false, precision = 7, scale = 4)
    private BigDecimal lngArred;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "estado", length = 100)
    private String estado;

    @Column(name = "cidade", length = 200)
    private String cidade;

    @Column(name = "bairro", length = 200)
    private String bairro;

    @Column(name = "logradouro", length = 300)
    private String logradouro;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "cep", length = 10)
    private String cep;

    @Column(name = "nome_local")
    private String nomeLocal;

    @Column(name = "tipo_local", length = 50)
    private String tipoLocal;

    @Column(name = "is_urbano")
    private Boolean isUrbano;

    @Column(name = "precisao_metros")
    private Integer precisaoMetros;

    @Column(name = "fonte", nullable = false, length = 50)
    @Builder.Default
    private String fonte = "NOMINATIM";

    @Column(name = "consulta_em", nullable = false)
    private LocalDateTime consultaEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;
}
