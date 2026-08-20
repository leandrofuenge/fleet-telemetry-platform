package com.telemetria.integration.sefaz.cte;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * Processador responsável por analisar a resposta XML/SOAP devolvida pela SEFAZ
 * e mapear para o DTO CteStatusResponse de forma segura e resiliente.
 */
@Component("sefazResponseParserProcessor")
public class SefazResponseParserProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(SefazResponseParserProcessor.class);

    private static final String CODIGO_ERRO_GENERICO = "999";
    private static final String CODIGO_SOAP_FAULT = "500";
    private static final String CODIGO_ERRO_CONEXAO = "503";

    private static final Pattern FAULT_STRING_PATTERN = Pattern.compile(
            "<faultstring[^>]*>(.*?)</faultstring>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final CteResponseParser responseParser;

    public SefazResponseParserProcessor(CteResponseParser responseParser) {
        this.responseParser = responseParser;
    }

    @Override
    public void process(Exchange exchange) {
        long startTime = obterPropertyLong(exchange, AuditLogProcessor.HEADER_START_TIME, 0L);
        long duration = (startTime > 0) ? (System.currentTimeMillis() - startTime) : 0L;

        String uf = obterMetadado(exchange, "SEFAZ_UF", "MT");
        String ambiente = obterMetadado(exchange, "SEFAZ_AMBIENTE_NOME", "HOMOLOGACAO");
        String xmlEnvioSoap = exchange.getProperty("SEFAZ_XML_ENVIO_SOAP", String.class);
        String xmlEnvioSoapBase64 = exchange.getProperty("SEFAZ_XML_ENVIO_SOAP_BASE64", String.class);
        boolean simulated = Boolean.TRUE.equals(exchange.getProperty("SEFAZ_SKIP_HTTP", Boolean.class));

        CteStatusResponse response = criarResponseBase(ambiente, uf, duration, simulated, xmlEnvioSoap, xmlEnvioSoapBase64);

        // 1. Verificação de Exceção Técnica lançada pela Rota Camel (Ex: Timeout, ConnRefused, SSLHandshake)
        Exception caughtException = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if (caughtException != null) {
            log.error("[Camel ResponseParser] Exceção capturada na rota de integração com SEFAZ UF {}: {}", uf, caughtException.getMessage());
            preencherErro(response, CODIGO_ERRO_CONEXAO, "Falha de comunicação/transmissão HTTP: " + caughtException.getMessage());
            exchange.getMessage().setBody(response);
            return;
        }

        String xmlResponse = exchange.getMessage().getBody(String.class);

        // 2. Tratamento para Resposta Vazia ou Nula
        if (xmlResponse == null || xmlResponse.isBlank()) {
            log.warn("[Camel ResponseParser] Resposta da SEFAZ veio vazia ou nula para UF: {}", uf);
            preencherErro(response, CODIGO_ERRO_GENERICO, "Resposta vazia ou nula recebida da SEFAZ");
            exchange.getMessage().setBody(response);
            return;
        }

        // 3. Processamento Defensivo do Payload (Extração de Inner XML e Base64)
        processarPayloadsSilenciosamente(response, xmlResponse);

        // 4. Tratamento Especializado de SOAP Fault com Extração do <faultstring>
        if (xmlResponse.contains(":Fault>") || xmlResponse.contains("<Fault>")) {
            String detalheFault = extrairFaultString(xmlResponse);
            log.error("[Camel ResponseParser] SOAP Fault detectado da SEFAZ UF {}: {}", uf, detalheFault);
            preencherErro(response, CODIGO_SOAP_FAULT, "SOAP Fault SEFAZ: " + detalheFault);
            exchange.getMessage().setBody(response);
            return;
        }

        // 5. Parsing do Negócio (cStat/xMotivo)
        try {
            CteStatusResultado resultado = responseParser.parseStatus(xmlResponse);
            response.setDisponivel(resultado.disponivel());
            response.setCodigo(String.valueOf(resultado.codigo()));
            response.setMensagem(resultado.motivo() != null && !resultado.motivo().isBlank()
                    ? resultado.motivo()
                    : "Resposta SEFAZ sem mensagem de motivo");

            log.info("[Camel ResponseParser] SEFAZ CT-e UF: {} -> cStat: {} ({}) em {}ms",
                    uf, response.getCodigo(), response.getMensagem(), duration);

        } catch (CteException e) {
            log.warn("[Camel ResponseParser] Resposta CT-e com rejeição fiscal na UF {}: {}", uf, e.getMessage());
            preencherErro(response, CODIGO_ERRO_GENERICO, e.getMessage());
        } catch (Exception e) {
            log.error("[Camel ResponseParser] Erro inesperado no parsing do XML da SEFAZ UF {}: {}", uf, e.getMessage(), e);
            preencherErro(response, CODIGO_ERRO_GENERICO, "Erro de conversão do XML da SEFAZ: " + e.getMessage());
        }

        exchange.getMessage().setBody(response);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE SUPORTE
    // =========================================================================

    private void processarPayloadsSilenciosamente(CteStatusResponse response, String xmlResponse) {
        try {
            String xmlResponseBase64 = Base64Utils.encode(xmlResponse);
            String innerXml = SoapEnvelopeHelper.extractInnerXml(xmlResponse);
            String innerXmlSeguro = (innerXml != null && !innerXml.isBlank()) ? innerXml : xmlResponse;
            String innerXmlBase64 = Base64Utils.encode(innerXmlSeguro);

            response.setXmlRetornoSoap(xmlResponse);
            response.setXmlRetornoSoapBase64(xmlResponseBase64);
            response.setXmlRetornoDados(innerXmlSeguro);
            response.setXmlRetornoDadosBase64(innerXmlBase64);
        } catch (Exception e) {
            log.warn("[Camel ResponseParser] Falha não bloqueante na extração do Inner XML ou Base64: {}", e.getMessage());
            response.setXmlRetornoSoap(xmlResponse);
        }
    }

    private String extrairFaultString(String xmlResponse) {
        try {
            Matcher matcher = FAULT_STRING_PATTERN.matcher(xmlResponse);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair faultstring via regex", e);
        }
        return "Falha na infraestrutura do WebService SOAP da SEFAZ";
    }

    private CteStatusResponse criarResponseBase(String ambiente, String uf, long duration, boolean simulated, String xmlEnvio, String xmlEnvioBase64) {
        CteStatusResponse response = new CteStatusResponse();
        response.setSistema("SEFAZ");
        response.setDocumento("CTE");
        response.setAmbiente(ambiente);
        response.setUf(uf);
        response.setTempoRespostaMs(duration);
        response.setSimulado(simulated);
        response.setXmlEnvioSoap(xmlEnvio);
        response.setXmlEnvioSoapBase64(xmlEnvioBase64);
        return response;
    }

    private void preencherErro(CteStatusResponse response, String codigo, String mensagem) {
        response.setDisponivel(false);
        response.setCodigo(codigo);
        response.setMensagem(mensagem);
    }

    private String obterMetadado(Exchange exchange, String chave, String valorPadrao) {
        String prop = exchange.getProperty(chave, String.class);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        String header = exchange.getMessage().getHeader(chave, String.class);
        return (header != null && !header.isBlank()) ? header : valorPadrao;
    }

    private long obterPropertyLong(Exchange exchange, String chave, long valorPadrao) {
        Long val = exchange.getProperty(chave, Long.class);
        return (val != null) ? val : valorPadrao;
    }
}