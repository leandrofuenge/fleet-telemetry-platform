package com.telemetria.integration.sefaz.nfe;

import org.springframework.stereotype.Component;

/** Bloqueia operações fiscais mutáveis até a liberação explícita em homologação. */
@Component
public class NfeFiscalOperationGuard {
    private final NfeProperties properties;

    public NfeFiscalOperationGuard(NfeProperties properties) { this.properties = properties; }

    public void exigirAutorizacaoPermitida() { exigir(properties.getOperations().isAuthorizationEnabled(), "autorização"); }
    public void exigirEventoPermitido() { exigir(properties.getOperations().isEventEnabled(), "evento"); }
    public void exigirInutilizacaoPermitida() { exigir(properties.getOperations().isInutilizationEnabled(), "inutilização"); }

    private void exigir(boolean enabled, String operation) {
        if (!enabled) throw new NfeException("Operação NF-e bloqueada (" + operation + "): desabilitada por configuração.");
        if (!properties.getOperations().isAuthorizedFiscalTestData()) {
            throw new NfeException("Operação NF-e bloqueada (" + operation + "): massa fiscal de teste não confirmada.");
        }
    }
}
