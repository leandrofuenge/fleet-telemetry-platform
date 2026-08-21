package com.telemetria.integration.sefaz.cte;

public final class CteFaultClassifier {
    public Classification classify(CteSoapFault fault) {
        if (fault == null) return Classification.UNKNOWN;
        String value = (fault.code() + " " + fault.reason()).toLowerCase(java.util.Locale.ROOT);
        if (value.contains("security") || value.contains("certificate")) return Classification.SECURITY;
        if (value.contains("timeout") || value.contains("unavailable")) return Classification.TRANSIENT;
        return Classification.BUSINESS;
    }
    public enum Classification { BUSINESS, SECURITY, TRANSIENT, UNKNOWN }
}
