package com.telemetria.integration.nfe.soap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NfeSoapServiceTest {

    @Test
    void deveExporContratoDeCadaOperacao() {
        for (NfeSoapService service : NfeSoapService.values()) {
            assertThat(service.namespace())
                    .isEqualTo("http://www.portalfiscal.inf.br/nfe/wsdl/" + service.servico());
            assertThat(service.soapAction())
                    .isEqualTo(service.namespace() + "/" + service.metodo());
            assertThat(service.elementoResposta()).isNotBlank();
        }
    }

    @Test
    void distribuicaoDfeDeveUsarContratoEncapsulado() {
        NfeSoapService service = NfeSoapService.DISTRIBUICAO_DFE;

        assertThat(service.requisicaoEncapsuladaPeloMetodo()).isTrue();
        assertThat(service.elementoResposta()).isEqualTo("nfeDistDFeInteresseResponse");
        assertThat(service.elementoResultado()).isEqualTo("nfeDistDFeInteresseResult");
    }
}
