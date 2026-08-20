package com.telemetria.integration.antt.rntrc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.exception.CteException;

@Component("rntrcClient")
public class RntrcClientImpl implements RntrcClient {

    @Value("${antt.rntrc.url-ondemand:https://rntrcservices-hml.bus.antt.gov.br/api/v1/RTRNC/SEFAZ/reenvio/ondemand}")
    private String endpointUrl;

    private static final String NAMESPACE_SCHEMA = "http://www.example.org/schema/1747748723278";

    @Override
    public RntrcReenvioResponse solicitarReenvioOnDemand(String placa, String cnpj) {
        if ((placa == null || placa.isBlank()) && (cnpj == null || cnpj.isBlank())) {
            throw new CteException("Deve ser informado ao menos a Placa ou o CNPJ/CPF para o reenvio sob demanda da ANTT.");
        }

        String soapEnvelope = construirEnvelopeSoap(placa, cnpj);
        String xmlResposta = enviarSoap(endpointUrl, soapEnvelope, "reenvio");

        return parseRespostaXml(xmlResposta);
    }

    private String construirEnvelopeSoap(String placa, String cnpj) {
        StringBuilder xml = new StringBuilder();
        xml.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        xml.append("xmlns:ns=\"").append(NAMESPACE_SCHEMA).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<ns:reenvioRequest>");
        
        if (placa != null && !placa.isBlank()) {
            xml.append("<placa>").append(placa.trim().toUpperCase()).append("</placa>");
        }
        if (cnpj != null && !cnpj.isBlank()) {
            xml.append("<cnpj>").append(cnpj.replaceAll("\\D", "")).append("</cnpj>");
        }

        xml.append("</ns:reenvioRequest>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");

        return xml.toString();
    }

    private String enviarSoap(String endpoint, String soapBody, String soapAction) {
        try {
            URL url = new URL(endpoint);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            // SOAP 1.1 exige Content-Type text/xml com SOAPAction
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", "\"" + soapAction + "\"");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = soapBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            var inputStream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } catch (Exception e) {
            throw new CteException("Falha na comunicação SOAP com o serviço RNTRC da ANTT: " + e.getMessage(), e);
        }
    }

    private RntrcReenvioResponse parseRespostaXml(String xmlResponse) {
        String cStat = extrairTag(xmlResponse, "cStat");
        String xMotivo = extrairTag(xmlResponse, "xMotivo");

        if (cStat == null) {
            cStat = "500";
        }
        if (xMotivo == null) {
            xMotivo = "Resposta inválida recebida da ANTT";
        }

        return new RntrcReenvioResponse(cStat, xMotivo);
    }

    private String extrairTag(String xml, String tagName) {
        Pattern pattern = Pattern.compile("<(?:.*?:)?" + tagName + ">(.*?)</(?:.*?:)?" + tagName + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}
