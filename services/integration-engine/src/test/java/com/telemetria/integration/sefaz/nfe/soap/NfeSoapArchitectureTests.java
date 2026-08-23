package com.telemetria.integration.sefaz.nfe.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.sefaz.nfe.NfeSefazUnavailableException;

class NfeSoapArchitectureTests {

    private final NfeSoapEnvelopeFactory envelopeFactory = new NfeSoapEnvelopeFactory();
    private final NfeSoapResponseValidator responseValidator = new NfeSoapResponseValidator();

    @Test
    void criaEnvelopeSoapDoServicoSelecionadoSemDeclaracaoXmlInterna() {
        String envelope = envelopeFactory.criar(NfeSoapService.CONSULTA,
                "<?xml version=\"1.0\"?><consSitNFe versao=\"4.00\"/>");

        assertThat(envelope).contains("<nfeDadosMsg xmlns=\"" + NfeSoapService.CONSULTA.namespace() + "\">");
        assertThat(envelope).contains("<consSitNFe versao=\"4.00\"/>");
        assertThat(envelope.substring(envelope.indexOf("<soap12:Body>"))).doesNotContain("<?xml");
        assertThat(NfeSoapService.EVENTO.soapAction()).endsWith("/NFeRecepcaoEvento4/nfeRecepcaoEvento");
    }

    @Test
    void rejeitaSoapFaultComMotivoDaSefaz() {
        String fault = """
                <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"><soap:Body>
                  <soap:Fault><soap:Reason><soap:Text>Ação inválida</soap:Text></soap:Reason></soap:Fault>
                </soap:Body></soap:Envelope>
                """;

        assertThatThrownBy(() -> responseValidator.validar(fault, NfeSoapService.EVENTO))
                .isInstanceOf(NfeSefazUnavailableException.class)
                .hasMessageContaining("Ação inválida");
    }

    @Test
    void aceitaEnvelopeSoapComBody() {
        String resposta = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap:Body><retConsStatServ versao=\"4.00\"/></soap:Body></soap:Envelope>";

        responseValidator.validar(resposta, NfeSoapService.STATUS);
    }
}
