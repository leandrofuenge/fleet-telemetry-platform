package com.telemetria.integration.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.telemetria.integration.sefaz.cte.soap.CteSoapService;

/**
 * Utilitário para encapsulamento e extração de documentos XML dentro de envelopes SOAP 1.2 para a SEFAZ.
 */
public final class SoapEnvelopeHelper {

    public static final String DEFAULT_CTE_NAMESPACE = "http://www.portalfiscal.inf.br/cte";
    public static final String DEFAULT_MDFE_NAMESPACE = "http://www.portalfiscal.inf.br/mdfe";
    public static final String DEFAULT_NFE_NAMESPACE = "http://www.portalfiscal.inf.br/nfe";

    private static final Pattern SOAP_BODY_PATTERN = Pattern.compile(
            "<(?i)(?:soap12|soap|s|soapenv):Body[^>]*>(.*?)</(?i)(?:soap12|soap|s|soapenv):Body>",
            Pattern.DOTALL
    );

    private static final Pattern CTE_DADOS_MSG_PATTERN = Pattern.compile(
            "<(?i)(?:cteDadosMsg|mdfeDadosMsg|nfeDadosMsg)[^>]*>(.*?)</(?i)(?:cteDadosMsg|mdfeDadosMsg|nfeDadosMsg)>",
            Pattern.DOTALL
    );

    private SoapEnvelopeHelper() {
    }

    /**
     * Envelopa o XML de negócio dentro do contexto SOAP 1.2 oficial com cteDadosMsg.
     */
    public static String wrapCteSoap12(String innerXml) {
        return wrapInSoap12(innerXml, "cteDadosMsg", DEFAULT_CTE_NAMESPACE);
    }

    /**
     * Envelopa uma mensagem CT-e 4.00 com o namespace WSDL específico do serviço.
     */
    public static String wrapCteSoap12(String innerXml, CteSoapService service) {
        if (service == null) {
            throw new IllegalArgumentException("O serviço SOAP do CT-e deve ser informado.");
        }
        return wrapInSoap12(innerXml, "cteDadosMsg", service.namespace());
    }

    /**
     * Envelopa o XML de negócio dentro de SOAP 1.2 com tag e namespace customizáveis.
     */
    public static String wrapInSoap12(String innerXml, String msgTag, String namespace) {
        String cleanInnerXml = (innerXml != null) ? innerXml.trim() : "";
        // Remove declaração <?xml...?> do conteúdo interno se já existir para evitar XML inválido
        if (cleanInnerXml.startsWith("<?xml")) {
            int endDecl = cleanInnerXml.indexOf("?>");
            if (endDecl != -1) {
                cleanInnerXml = cleanInnerXml.substring(endDecl + 2).trim();
            }
        }

        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
                  <soap12:Body>
                    <%s xmlns="%s">
                      %s
                    </%s>
                  </soap12:Body>
                </soap12:Envelope>
                """.formatted(msgTag, namespace, cleanInnerXml, msgTag).trim();
    }

    /**
     * Extrai o XML de dados de dentro do envelope SOAP e da tag de dados.
     */
    public static String extractInnerXml(String soapXml) {
        if (soapXml == null || soapXml.isBlank()) {
            return "";
        }

        // Tenta extrair de dentro de cteDadosMsg / mdfeDadosMsg
        Matcher dadosMatcher = CTE_DADOS_MSG_PATTERN.matcher(soapXml);
        if (dadosMatcher.find()) {
            return dadosMatcher.group(1).trim();
        }

        // Tenta extrair de dentro do soap:Body
        Matcher bodyMatcher = SOAP_BODY_PATTERN.matcher(soapXml);
        if (bodyMatcher.find()) {
            return bodyMatcher.group(1).trim();
        }

        return soapXml.trim();
    }
}
