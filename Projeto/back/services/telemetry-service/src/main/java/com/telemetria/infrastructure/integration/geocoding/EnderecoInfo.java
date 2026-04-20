package com.telemetria.infrastructure.integration.geocoding;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * DTO para dados de endereço do OpenStreetMap
 */
public class EnderecoInfo {
    private String displayName;
    private String cidade;
    private String estado;
    private String pais;
    private String rua;
    private String cep;
    private String bairro;

    // Construtores
    public EnderecoInfo() {}

    public EnderecoInfo(JsonNode json) {
        if (json != null) {
            this.displayName = json.has("display_name") ? json.get("display_name").asText() : null;
            
            if (json.has("address")) {
                JsonNode address = json.get("address");
                this.cidade = getFromAddress(address, "city", "town", "village");
                this.estado = getFromAddress(address, "state");
                this.pais = getFromAddress(address, "country");
                this.rua = getFromAddress(address, "road", "pedestrian");
                this.cep = getFromAddress(address, "postcode");
                this.bairro = getFromAddress(address, "suburb", "neighbourhood");
            }
        }
    }

    private String getFromAddress(JsonNode address, String... keys) {
        for (String key : keys) {
            if (address.has(key)) {
                return address.get(key).asText();
            }
        }
        return null;
    }

    // Getters e Setters
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    @Override
    public String toString() {
        return String.format("EnderecoInfo{cidade='%s', estado='%s', pais='%s', rua='%s', cep='%s', bairro='%s'}",
                cidade, estado, pais, rua, cep, bairro);
    }
}