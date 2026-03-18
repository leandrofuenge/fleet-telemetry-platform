package com.app.telemetria.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "geocoding_cache", uniqueConstraints = @UniqueConstraint(name = "uk_geocoding_coords", columnNames = {
        "lat_arred", "lng_arred" }), indexes = @Index(name = "idx_geo_expira", columnList = "expira_em"))
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
    private String fonte = "NOMINATIM";

    @Column(name = "consulta_em", nullable = false)
    private LocalDateTime consultaEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    // ================================
    // Construtores
    // ================================

    public GeocodingCache() {
    }

    public GeocodingCache(Long id, BigDecimal latArred, BigDecimal lngArred, String pais, 
                         String estado, String cidade, String bairro, String logradouro, 
                         String numero, String cep, String nomeLocal, String tipoLocal, 
                         Boolean isUrbano, Integer precisaoMetros, String fonte, 
                         LocalDateTime consultaEm, LocalDateTime expiraEm) {
        this.id = id;
        this.latArred = latArred;
        this.lngArred = lngArred;
        this.pais = pais;
        this.estado = estado;
        this.cidade = cidade;
        this.bairro = bairro;
        this.logradouro = logradouro;
        this.numero = numero;
        this.cep = cep;
        this.nomeLocal = nomeLocal;
        this.tipoLocal = tipoLocal;
        this.isUrbano = isUrbano;
        this.precisaoMetros = precisaoMetros;
        this.fonte = fonte != null ? fonte : "NOMINATIM";
        this.consultaEm = consultaEm;
        this.expiraEm = expiraEm;
    }

    // ================================
    // Getters e Setters
    // ================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getLatArred() {
        return latArred;
    }

    public void setLatArred(BigDecimal latArred) {
        if (latArred != null) {
            // Garantir precisão 7,4
            latArred = latArred.setScale(4, BigDecimal.ROUND_HALF_UP);
        }
        this.latArred = latArred;
    }

    public BigDecimal getLngArred() {
        return lngArred;
    }

    public void setLngArred(BigDecimal lngArred) {
        if (lngArred != null) {
            // Garantir precisão 7,4
            lngArred = lngArred.setScale(4, BigDecimal.ROUND_HALF_UP);
        }
        this.lngArred = lngArred;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getNomeLocal() {
        return nomeLocal;
    }

    public void setNomeLocal(String nomeLocal) {
        this.nomeLocal = nomeLocal;
    }

    public String getTipoLocal() {
        return tipoLocal;
    }

    public void setTipoLocal(String tipoLocal) {
        this.tipoLocal = tipoLocal;
    }

    public Boolean getIsUrbano() {
        return isUrbano;
    }

    public void setIsUrbano(Boolean isUrbano) {
        this.isUrbano = isUrbano;
    }

    public Integer getPrecisaoMetros() {
        return precisaoMetros;
    }

    public void setPrecisaoMetros(Integer precisaoMetros) {
        this.precisaoMetros = precisaoMetros;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte != null ? fonte : "NOMINATIM";
    }

    public LocalDateTime getConsultaEm() {
        return consultaEm;
    }

    public void setConsultaEm(LocalDateTime consultaEm) {
        this.consultaEm = consultaEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(LocalDateTime expiraEm) {
        this.expiraEm = expiraEm;
    }

    // ================================
    // Métodos auxiliares
    // ================================

    /**
     * Verifica se o cache ainda é válido
     */
    public boolean isValido() {
        return expiraEm != null && expiraEm.isAfter(LocalDateTime.now());
    }

    /**
     * Retorna o endereço formatado
     */
    public String getEnderecoFormatado() {
        StringBuilder sb = new StringBuilder();
        
        if (logradouro != null && !logradouro.isEmpty()) {
            sb.append(logradouro);
            if (numero != null && !numero.isEmpty()) {
                sb.append(", ").append(numero);
            }
        }
        
        if (bairro != null && !bairro.isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(bairro);
        }
        
        if (cidade != null && !cidade.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(cidade);
        }
        
        if (estado != null && !estado.isEmpty()) {
            sb.append(" - ").append(estado);
        }
        
        if (cep != null && !cep.isEmpty()) {
            sb.append(" - CEP: ").append(cep);
        }
        
        if (pais != null && !pais.isEmpty() && !"Brasil".equalsIgnoreCase(pais)) {
            sb.append(" - ").append(pais);
        }
        
        return sb.toString();
    }

    /**
     * Retorna uma representação compacta do endereço
     */
    public String getEnderecoCompacto() {
        if (logradouro != null && !logradouro.isEmpty()) {
            if (cidade != null && !cidade.isEmpty()) {
                return logradouro + ", " + cidade;
            }
            return logradouro;
        }
        if (cidade != null && !cidade.isEmpty()) {
            return cidade + " - " + (estado != null ? estado : "");
        }
        return "";
    }

    // ================================
    // Builder manual
    // ================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private BigDecimal latArred;
        private BigDecimal lngArred;
        private String pais;
        private String estado;
        private String cidade;
        private String bairro;
        private String logradouro;
        private String numero;
        private String cep;
        private String nomeLocal;
        private String tipoLocal;
        private Boolean isUrbano;
        private Integer precisaoMetros;
        private String fonte = "NOMINATIM";
        private LocalDateTime consultaEm;
        private LocalDateTime expiraEm;

        Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder latArred(BigDecimal latArred) {
            if (latArred != null) {
                this.latArred = latArred.setScale(4, BigDecimal.ROUND_HALF_UP);
            }
            return this;
        }

        public Builder lngArred(BigDecimal lngArred) {
            if (lngArred != null) {
                this.lngArred = lngArred.setScale(4, BigDecimal.ROUND_HALF_UP);
            }
            return this;
        }

        public Builder pais(String pais) {
            this.pais = pais;
            return this;
        }

        public Builder estado(String estado) {
            this.estado = estado;
            return this;
        }

        public Builder cidade(String cidade) {
            this.cidade = cidade;
            return this;
        }

        public Builder bairro(String bairro) {
            this.bairro = bairro;
            return this;
        }

        public Builder logradouro(String logradouro) {
            this.logradouro = logradouro;
            return this;
        }

        public Builder numero(String numero) {
            this.numero = numero;
            return this;
        }

        public Builder cep(String cep) {
            this.cep = cep;
            return this;
        }

        public Builder nomeLocal(String nomeLocal) {
            this.nomeLocal = nomeLocal;
            return this;
        }

        public Builder tipoLocal(String tipoLocal) {
            this.tipoLocal = tipoLocal;
            return this;
        }

        public Builder isUrbano(Boolean isUrbano) {
            this.isUrbano = isUrbano;
            return this;
        }

        public Builder precisaoMetros(Integer precisaoMetros) {
            this.precisaoMetros = precisaoMetros;
            return this;
        }

        public Builder fonte(String fonte) {
            this.fonte = fonte != null ? fonte : "NOMINATIM";
            return this;
        }

        public Builder consultaEm(LocalDateTime consultaEm) {
            this.consultaEm = consultaEm;
            return this;
        }

        public Builder expiraEm(LocalDateTime expiraEm) {
            this.expiraEm = expiraEm;
            return this;
        }

        public GeocodingCache build() {
            return new GeocodingCache(
                this.id, this.latArred, this.lngArred, this.pais, this.estado,
                this.cidade, this.bairro, this.logradouro, this.numero, this.cep,
                this.nomeLocal, this.tipoLocal, this.isUrbano, this.precisaoMetros,
                this.fonte, this.consultaEm, this.expiraEm
            );
        }
    }
}
