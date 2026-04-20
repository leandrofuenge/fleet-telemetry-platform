package com.app.routing.enums;

// ============================================================
// ARQUIVO: PerfilOsrm.java
// RN-ROT-001 — Perfil OSRM. CAMINHAO é o padrão obrigatório.
// pathSegment é a string literal usada na URL do OSRM:
//   /route/v1/{pathSegment}/lon,lat;lon,lat
// ============================================================

public enum PerfilOsrm {
    CAMINHAO("caminhao"),
    CARRO("driving");

    private final String pathSegment;

    PerfilOsrm(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    /**
     * Retorna o segmento de path da URL do OSRM correspondente ao perfil.
     * Exemplo: CAMINHAO → "caminhao", CARRO → "driving"
     */
    public String getPathSegment() {
        return pathSegment;
    }
}

