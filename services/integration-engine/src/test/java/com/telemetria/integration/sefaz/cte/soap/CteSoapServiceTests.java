package com.telemetria.integration.sefaz.cte.soap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.util.SoapEnvelopeHelper;

class CteSoapServiceTests {

    @Test
    void deveGerarEnvelopeComNamespaceEspecificoDeCadaServico() {
        for (CteSoapService service : CteSoapService.values()) {
            String envelope = SoapEnvelopeHelper.wrapCteSoap12("<teste/>", service);

            assertTrue(envelope.contains("<cteDadosMsg xmlns=\"" + service.namespace() + "\">"));
            assertTrue(envelope.contains("<teste/>"));
        }
    }

    @Test
    void deveDefinirAcoesSoapDoCte400() {
        assertTrue(CteSoapService.AUTORIZACAO.soapAction().endsWith("/CTeRecepcaoSincV4/cteRecepcao"));
        assertTrue(CteSoapService.CONSULTA.soapAction().endsWith("/CTeConsultaV4/cteConsultaCT"));
        assertTrue(CteSoapService.EVENTO.soapAction().endsWith("/CTeRecepcaoEventoV4/cteRecepcaoEvento"));
        assertTrue(CteSoapService.STATUS.soapAction().endsWith("/CTeStatusServicoV4/cteStatusServicoCT"));
    }

    @Test
    void deveRemoverDeclaracaoXmlDoDocumentoInterno() {
        String envelope = SoapEnvelopeHelper.wrapCteSoap12(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><consSitCTe/>", CteSoapService.CONSULTA);

        assertTrue(envelope.contains("<consSitCTe/>"));
        assertFalse(envelope.substring(envelope.indexOf("<soap12:Body>"))
                .contains("<?xml version="));
    }

    @Test
    void deveRejeitarServicoAusente() {
        assertThrows(IllegalArgumentException.class,
                () -> SoapEnvelopeHelper.wrapCteSoap12("<teste/>", null));
    }
}
