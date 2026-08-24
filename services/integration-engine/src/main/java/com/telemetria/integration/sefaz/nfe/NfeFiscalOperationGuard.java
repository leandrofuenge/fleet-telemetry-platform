package com.telemetria.integration.sefaz.nfe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Bloqueia operações fiscais mutáveis até a liberação explícita em homologação. */
@Component
public class NfeFiscalOperationGuard {
    private static final Logger log = LoggerFactory.getLogger(NfeFiscalOperationGuard.class);
    private final NfeProperties properties;

    public NfeFiscalOperationGuard(NfeProperties properties) { this.properties = properties; }

    public void exigirAutorizacaoPermitida() { exigir(properties.getOperations().isAuthorizationEnabled(), "autorização"); }
    public void exigirEventoPermitido() { exigir(properties.getOperations().isEventEnabled(), "evento"); }
    public void exigirInutilizacaoPermitida() { exigir(properties.getOperations().isInutilizationEnabled(), "inutilização"); }

    private void exigir(boolean enabled, String operation) {
        if (!enabled) {
            log.warn("NF-e: operação {} bloqueada por configuração", operation);
            throw new NfeOperationBlockedException(
                    "Operação NF-e bloqueada (" + operation + "): desabilitada por configuração.");
        }
        if (!properties.getOperations().isAuthorizedFiscalTestData()) {
            log.warn("NF-e: operação {} bloqueada porque a massa fiscal não foi confirmada", operation);
            throw new NfeOperationBlockedException(
                    "Operação NF-e bloqueada (" + operation + "): massa fiscal de teste não confirmada.");
        }
        log.info("NF-e: operação {} liberada pelas proteções fiscais", operation);
    }
}
