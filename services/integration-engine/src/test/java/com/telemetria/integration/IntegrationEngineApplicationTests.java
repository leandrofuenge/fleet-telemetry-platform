package com.telemetria.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.telemetria.integration.sefaz.cte.route.CteRoute;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;
import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

@SpringBootTest(properties = "integration.simulation.enabled=true")
class IntegrationEngineApplicationTests {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    @DisplayName("Deve carregar o contexto do Spring Boot e Apache Camel com sucesso")
    void contextLoads() {
        assertThat(producerTemplate).isNotNull();
    }

    @Test
    @DisplayName("Deve validar codificação e decodificação Base64 e GZIP")
    void deveValidarUtilitariosBase64() throws Exception {
        String xmlExemplo = "<CTe><infCte Id=\"CTe51260800000000000000570010000000011000000010\"><vPrest><vTPrest>1500.00</vTPrest></vPrest></infCte></CTe>";

        String base64 = Base64Utils.encode(xmlExemplo);
        assertThat(base64).isNotBlank();

        String decodificado = Base64Utils.decodeToString(base64);
        assertThat(decodificado).isEqualTo(xmlExemplo);

        String gzipBase64 = Base64Utils.compressGzipBase64(xmlExemplo);
        assertThat(gzipBase64).isNotBlank();

        String decompresso = Base64Utils.decompressGzipBase64(gzipBase64);
        assertThat(decompresso).isEqualTo(xmlExemplo);
    }

    @Test
    @DisplayName("Deve envelopar e extrair XML dentro do contexto SOAP 1.2")
    void deveEnveloparSoapContext() {
        String xmlInterno = "<consStatServCTe versao=\"4.00\" xmlns=\"http://www.portalfiscal.inf.br/cte\"><tpAmb>2</tpAmb><cUF>51</cUF><xServ>STATUS</xServ></consStatServCTe>";

        String envelopeSoap = SoapEnvelopeHelper.wrapCteSoap12(xmlInterno);
        assertThat(envelopeSoap).contains("<soap12:Envelope");
        assertThat(envelopeSoap).contains("<soap12:Body>");
        assertThat(envelopeSoap).contains("<cteDadosMsg xmlns=\"http://www.portalfiscal.inf.br/cte\">");
        assertThat(envelopeSoap).contains("<consStatServCTe");

        String extraido = SoapEnvelopeHelper.extractInnerXml(envelopeSoap);
        assertThat(extraido).contains("<consStatServCTe");
    }

    @Test
    @DisplayName("Deve executar a rota Camel CT-e 4.00 com contexto SOAP e geração de Base64 no envio e retorno")
    void deveExecutarRotaSefazCteStatusComXmlEBase64() {
        CteStatusRequest request = new CteStatusRequest("MT", "homologacao");

        CteStatusResponse response = producerTemplate.requestBody(
                CteRoute.ROUTE_CTE_STATUS,
                request,
                CteStatusResponse.class
        );

        assertThat(response).isNotNull();
        assertThat(response.getSistema()).isEqualTo("SEFAZ");
        assertThat(response.getDocumento()).isEqualTo("CTE");
        assertThat(response.getUf()).isEqualTo("MT");
        assertThat(response.getCodigo()).isNotNull();

        // Validação das propriedades XML e Base64 no contexto SOAP
        assertThat(response.getXmlEnvioSoap()).contains("<soap12:Envelope");
        assertThat(response.getXmlEnvioSoapBase64()).isNotBlank();
        assertThat(response.getXmlRetornoSoap()).isNotNull();
        assertThat(response.getXmlRetornoSoapBase64()).isNotBlank();
        assertThat(response.getXmlRetornoDados()).isNotNull();
        assertThat(response.getXmlRetornoDadosBase64()).isNotBlank();
    }

}
