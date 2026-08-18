package com.telemetria.integration.sefaz.cte;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

@Component("sefazSoapEnvelopeProcessor")
public class SefazSoapEnvelopeProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(SefazSoapEnvelopeProcessor.class);
    private final SefazProperties sefazProperties;

    private static final Map<String, String> UF_IBGE_MAP = Map.ofEntries(
            Map.entry("RO", "11"), Map.entry("AC", "12"), Map.entry("AM", "13"), Map.entry("RR", "14"),
            Map.entry("PA", "15"), Map.entry("AP", "16"), Map.entry("TO", "17"), Map.entry("MA", "21"),
            Map.entry("PI", "22"), Map.entry("CE", "23"), Map.entry("RN", "24"), Map.entry("PB", "25"),
            Map.entry("PE", "26"), Map.entry("AL", "27"), Map.entry("SE", "28"), Map.entry("BA", "29"),
            Map.entry("MG", "31"), Map.entry("ES", "32"), Map.entry("RJ", "33"), Map.entry("SP", "35"),
            Map.entry("PR", "41"), Map.entry("SC", "42"), Map.entry("RS", "43"), Map.entry("MS", "50"),
            Map.entry("MT", "51"), Map.entry("GO", "52"), Map.entry("DF", "53")
    );

    public SefazSoapEnvelopeProcessor(SefazProperties sefazProperties) {
        this.sefazProperties = sefazProperties;
    }

    @Override
    public void process(Exchange exchange) {
        CteStatusRequest request = exchange.getIn().getBody(CteStatusRequest.class);
        if (request == null) {
            request = new CteStatusRequest();
        }

        String uf = (request.getUf() != null) ? request.getUf().toUpperCase() : "MT";
        String cUF = UF_IBGE_MAP.getOrDefault(uf, "51");

        String ambienteCfg = (request.getAmbiente() != null) 
                ? request.getAmbiente() 
                : sefazProperties.getCte().getAmbiente();
        String tpAmb = ("producao".equalsIgnoreCase(ambienteCfg) || "1".equals(ambienteCfg)) ? "1" : "2";

        String versao = sefazProperties.getCte().getVersao();

        // Armazena metadados no Exchange
        exchange.setProperty("SEFAZ_UF", uf);
        exchange.setProperty("SEFAZ_CUF", cUF);
        exchange.setProperty("SEFAZ_TP_AMB", tpAmb);
        exchange.setProperty("SEFAZ_AMBIENTE_NOME", "1".equals(tpAmb) ? "PRODUCAO" : "HOMOLOGACAO");

        // Constrói XML do pedido interno de status do CT-e
        String innerXml = """
                <consStatServCTe versao="%s" xmlns="http://www.portalfiscal.inf.br/cte">
                  <tpAmb>%s</tpAmb>
                  <cUF>%s</cUF>
                  <xServ>STATUS</xServ>
                </consStatServCTe>
                """.formatted(versao, tpAmb, cUF).trim();

        // Envelopa o XML no contexto oficial SOAP 1.2
        String soapXml = SoapEnvelopeHelper.wrapCteSoap12(innerXml);
        String soapXmlBase64 = Base64Utils.encode(soapXml);

        exchange.setProperty("SEFAZ_XML_ENVIO_SOAP", soapXml);
        exchange.setProperty("SEFAZ_XML_ENVIO_SOAP_BASE64", soapXmlBase64);

        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/soap+xml; charset=utf-8");
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setBody(soapXml);

        log.info("[Camel SefazProcessor] Contexto SOAP 1.2 CT-e gerado para UF: {} (IBGE: {}), Ambiente: {} (Base64 length: {})",
                uf, cUF, "1".equals(tpAmb) ? "PRODUCAO" : "HOMOLOGACAO", soapXmlBase64.length());
    }
}
