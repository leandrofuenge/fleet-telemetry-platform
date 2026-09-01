package com.telemetria.integration.nfe.soap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.domain.exception.NfeSefazUnavailableException;

class NfeSoapResponseValidatorTest {

    private static final String SOAP_NS = "http://www.w3.org/2003/05/soap-envelope";
    private final NfeSoapResponseValidator validator = new NfeSoapResponseValidator();

    @Test
    void deveAceitarRespostaPadraoValida() {
        String xml = envelope(
                "<nfeResultMsg xmlns=\"" + NfeSoapService.STATUS.namespace() + "\">"
                        + "<retConsStatServ/></nfeResultMsg>");

        assertThatCode(() -> validator.validar(xml, NfeSoapService.STATUS))
                .doesNotThrowAnyException();
    }

    @Test
    void deveAceitarRespostaValidaDaDistribuicaoDfe() {
        NfeSoapService service = NfeSoapService.DISTRIBUICAO_DFE;
        String xml = envelope(
                "<nfeDistDFeInteresseResponse xmlns=\"" + service.namespace() + "\">"
                        + "<nfeDistDFeInteresseResult><retDistDFeInt/></nfeDistDFeInteresseResult>"
                        + "</nfeDistDFeInteresseResponse>");

        assertThatCode(() -> validator.validar(xml, service)).doesNotThrowAnyException();
    }

    @Test
    void deveRejeitarNamespaceSoapIncorretoOuBodyVazio() {
        assertThatThrownBy(() -> validator.validar(
                "<Envelope xmlns=\"urn:fake\"><Body/></Envelope>",
                NfeSoapService.STATUS))
                .isInstanceOf(NfeSefazUnavailableException.class);

        assertThatThrownBy(() -> validator.validar(envelope(""), NfeSoapService.STATUS))
                .isInstanceOf(NfeSefazUnavailableException.class)
                .hasMessageContaining("elemento esperado");
    }

    @Test
    void deveConverterSoapFaultETruncarMensagem() {
        String detalhe = "x".repeat(600);
        String xml = envelope(
                "<s:Fault><s:Reason><s:Text>" + detalhe
                        + "</s:Text></s:Reason></s:Fault>");

        assertThatThrownBy(() -> validator.validar(xml, NfeSoapService.STATUS))
                .isInstanceOf(NfeSefazUnavailableException.class)
                .hasMessageContaining("SOAP Fault")
                .hasMessageContaining("...");
    }

    @Test
    void deveBloquearDoctype() {
        String xml = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + envelope("<nfeResultMsg xmlns=\"" + NfeSoapService.STATUS.namespace()
                        + "\">&xxe;</nfeResultMsg>");

        assertThatThrownBy(() -> validator.validar(xml, NfeSoapService.STATUS))
                .isInstanceOf(NfeSefazUnavailableException.class)
                .hasMessageContaining("Resposta SOAP inválida");
    }

    private String envelope(String body) {
        return "<s:Envelope xmlns:s=\"" + SOAP_NS + "\"><s:Body>"
                + body + "</s:Body></s:Envelope>";
    }
}
