package com.telemetria.integration.sefaz.cte;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.retorno.CteStatusResultado;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;
import com.telemetria.integration.support.AuditLogProcessor;
import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

@Component("sefazResponseParserProcessor")
public class SefazResponseParserProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(SefazResponseParserProcessor.class);
    private final CteResponseParser responseParser;

    public SefazResponseParserProcessor(CteResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    @Override
    public void process(Exchange exchange) {
        String xmlResponse = exchange.getIn().getBody(String.class);
        String uf = exchange.getProperty("SEFAZ_UF", "MT", String.class);
        String ambiente = exchange.getProperty("SEFAZ_AMBIENTE_NOME", "HOMOLOGACAO", String.class);
        String xmlEnvioSoap = exchange.getProperty("SEFAZ_XML_ENVIO_SOAP", String.class);
        String xmlEnvioSoapBase64 = exchange.getProperty("SEFAZ_XML_ENVIO_SOAP_BASE64", String.class);
        Long startTime = exchange.getProperty(AuditLogProcessor.HEADER_START_TIME, Long.class);
        boolean simulated = Boolean.TRUE.equals(
                exchange.getProperty("SEFAZ_SKIP_HTTP", Boolean.class));
        long duration = (startTime != null) ? (System.currentTimeMillis() - startTime) : 0L;

        if (xmlResponse == null || xmlResponse.isBlank()) {
            CteStatusResponse fallback = new CteStatusResponse(
                    ambiente, uf, false, "999", "Resposta vazia da SEFAZ", duration
            );
            fallback.setXmlEnvioSoap(xmlEnvioSoap);
            fallback.setXmlEnvioSoapBase64(xmlEnvioSoapBase64);
            fallback.setSimulado(simulated);
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
        response.setSimulado(simulated);
        response.setXmlEnvioSoap(xmlEnvioSoap);
        response.setXmlEnvioSoapBase64(xmlEnvioSoapBase64);
        response.setXmlRetornoSoap(xmlResponse);
        response.setXmlRetornoSoapBase64(xmlResponseBase64);
        response.setXmlRetornoDados(innerXml);
        response.setXmlRetornoDadosBase64(innerXmlBase64);

        try {
            CteStatusResultado resultado = responseParser.parseStatus(xmlResponse);
            response.setDisponivel(resultado.disponivel());
            response.setCodigo(String.valueOf(resultado.codigo()));
            response.setMensagem(resultado.motivo() != null
                    ? resultado.motivo() : "Resposta SEFAZ sem código de status");

            log.info("[Camel ResponseParser] SEFAZ CT-e UF: {} -> cStat: {} ({}) em {}ms (Payload Base64: {} bytes)",
                    uf, response.getCodigo(), response.getMensagem(), duration, innerXmlBase64.length());

        } catch (CteException e) {
            log.warn("[Camel ResponseParser] Resposta CT-e inválida: {}", e.getMessage());
            response.setDisponivel(false);
            response.setCodigo("999");
            response.setMensagem(e.getMessage());
        }

        exchange.getIn().setBody(response);
    }

}
