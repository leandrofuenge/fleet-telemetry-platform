package com.telemetria.integration.processor;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;
import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

@Component("sefazResponseParserProcessor")
public class SefazResponseParserProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(SefazResponseParserProcessor.class);

    @Override
    public void process(Exchange exchange) {
        String xmlResponse = exchange.getIn().getBody(String.class);
        String uf = exchange.getProperty("SEFAZ_UF", "MT", String.class);
        String ambiente = exchange.getProperty("SEFAZ_AMBIENTE_NOME", "HOMOLOGACAO", String.class);
        String xmlEnvioSoap = exchange.getProperty("SEFAZ_XML_ENVIO_SOAP", String.class);
        String xmlEnvioSoapBase64 = exchange.getProperty("SEFAZ_XML_ENVIO_SOAP_BASE64", String.class);
        Long startTime = exchange.getProperty(AuditLogProcessor.HEADER_START_TIME, Long.class);
        long duration = (startTime != null) ? (System.currentTimeMillis() - startTime) : 0L;

        if (xmlResponse == null || xmlResponse.isBlank()) {
            CteStatusResponse fallback = new CteStatusResponse(
                    ambiente, uf, false, "999", "Resposta vazia da SEFAZ", duration
            );
            fallback.setXmlEnvioSoap(xmlEnvioSoap);
            fallback.setXmlEnvioSoapBase64(xmlEnvioSoapBase64);
            exchange.getIn().setBody(fallback);
            return;
        }

        String innerXml = SoapEnvelopeHelper.extractInnerXml(xmlResponse);
        String xmlResponseBase64 = Base64Utils.encode(xmlResponse);
        String innerXmlBase64 = Base64Utils.encode(innerXml);

        CteStatusResponse response = new CteStatusResponse();
        response.setSistema("SEFAZ");
        response.setDocumento("CTE");
        response.setAmbiente(ambiente);
        response.setUf(uf);
        response.setTempoRespostaMs(duration);
        response.setXmlEnvioSoap(xmlEnvioSoap);
        response.setXmlEnvioSoapBase64(xmlEnvioSoapBase64);
        response.setXmlRetornoSoap(xmlResponse);
        response.setXmlRetornoSoapBase64(xmlResponseBase64);
        response.setXmlRetornoDados(innerXml);
        response.setXmlRetornoDadosBase64(innerXmlBase64);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Proteções contra XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            String cStat = extractTagValue(doc, "cStat");
            String xMotivo = extractTagValue(doc, "xMotivo");

            boolean disponivel = "107".equals(cStat);

            response.setDisponivel(disponivel);
            response.setCodigo(cStat != null ? cStat : "107");
            response.setMensagem(xMotivo != null ? xMotivo : "Serviço em Operação");

            log.info("[Camel ResponseParser] SEFAZ CT-e UF: {} -> cStat: {} ({}) em {}ms (Payload Base64: {} bytes)",
                    uf, response.getCodigo(), response.getMensagem(), duration, innerXmlBase64.length());

        } catch (Exception e) {
            log.warn("[Camel ResponseParser] Falha no parse DOM do XML retornado (usando extração regex): {}", e.getMessage());

            String cStat = extractByRegex(xmlResponse, "cStat", "107");
            String xMotivo = extractByRegex(xmlResponse, "xMotivo", "Serviço em Operação");

            response.setDisponivel("107".equals(cStat));
            response.setCodigo(cStat);
            response.setMensagem(xMotivo);
        }

        exchange.getIn().setBody(response);
    }

    private String extractTagValue(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagNameNS("*", tagName);
        if (list.getLength() == 0) {
            list = doc.getElementsByTagName(tagName);
        }
        if (list.getLength() > 0 && list.item(0) != null) {
            return list.item(0).getTextContent();
        }
        return null;
    }

    private String extractByRegex(String xml, String tag, String defaultValue) {
        try {
            Matcher matcher = Pattern.compile("<(?:" + tag + "|.*:" + tag + ")>(.*?)</(?:" + tag + "|.*:" + tag + ")>")
                    .matcher(xml);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }
}
