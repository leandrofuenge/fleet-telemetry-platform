package com.telemetria.integration.sefaz.nfe;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NfeFiscalOperationGuardTests {

    @Test
    void bloqueiaAutorizacaoEnquantoNaoHouverLiberacaoExplicita() {
        NfeProperties properties = new NfeProperties();
        properties.getOperations().setAuthorizationEnabled(false);
        properties.getOperations().setAuthorizedFiscalTestData(true);

        assertThatThrownBy(() -> new NfeFiscalOperationGuard(properties).exigirAutorizacaoPermitida())
                .isInstanceOf(NfeException.class)
                .hasMessageContaining("desabilitada");
    }

    @Test
    void exigeConfirmacaoDaMassaFiscalMesmoComOperacaoHabilitada() {
        NfeProperties properties = new NfeProperties();
        properties.getOperations().setAuthorizationEnabled(true);
        properties.getOperations().setAuthorizedFiscalTestData(false);

        assertThatThrownBy(() -> new NfeFiscalOperationGuard(properties).exigirAutorizacaoPermitida())
                .isInstanceOf(NfeException.class)
                .hasMessageContaining("massa fiscal");
    }
}
