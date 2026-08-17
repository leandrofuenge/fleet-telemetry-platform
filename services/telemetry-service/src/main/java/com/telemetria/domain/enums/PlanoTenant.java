package com.telemetria.domain.enums;

public enum PlanoTenant {
    STARTER(9, false, false, false),
    PRO(49, true, false, false),
    ENTERPRISE(499, true, true, true),
    CUSTOM(Integer.MAX_VALUE, true, true, true);

    private final int maxVeiculos;
    private final boolean osrmHabilitado;
    private final boolean iaHabilitada;
    private final boolean apiHabilitada;

    PlanoTenant(int maxVeiculos, boolean osrmHabilitado, boolean iaHabilitada, boolean apiHabilitada) {
        this.maxVeiculos = maxVeiculos;
        this.osrmHabilitado = osrmHabilitado;
        this.iaHabilitada = iaHabilitada;
        this.apiHabilitada = apiHabilitada;
    }

    public int getMaxVeiculos() { return maxVeiculos; }
    public boolean isOsrmHabilitado() { return osrmHabilitado; }
    public boolean isIaHabilitada() { return iaHabilitada; }
    public boolean isApiHabilitada() { return apiHabilitada; }
}
