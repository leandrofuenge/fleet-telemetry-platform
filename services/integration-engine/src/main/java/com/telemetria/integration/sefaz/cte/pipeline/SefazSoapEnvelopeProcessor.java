package com.telemetria.integration.sefaz.cte.pipeline;

import java.util.Map;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.cte.domain.CteAmbiente;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.soap.CteSoapService;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.validation.CteXmlValidator;
import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

/**
 * Processador responsável por construir o payload XML de consulta de status
 * e encapsulá-lo no Envelope SOAP 1.2 específico do CT-e.
 */
@Component("sefazSoapEnvelopeProcessor")
public class SefazSoapEnvelopeProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(SefazSoapEnvelopeProcessor.class);
    
    private final SefazProperties sefazProperties;
    private final CteXmlValidator cteXmlValidator;

    private static final Map<String, String> UF_IBGE_MAP = Map.ofEntries(
            Map.entry("RO", "11"), Map.entry("AC", "12"), Map.entry("AM", "13"), Map.entry("RR", "14"),
            Map.entry("PA", "15"), Map.entry("AP", "16"), Map.entry("TO", "17"), Map.entry("MA", "21"),
            Map.entry("PI", "22"), Map.entry("CE", "23"), Map.entry("RN", "24"), Map.entry("PB", "25"),
            Map.entry("PE", "26"), Map.entry("AL", "27"), Map.entry("SE", "28"), Map.entry("BA", "29"),
            Map.entry("MG", "31"), Map.entry("ES", "32"), Map.entry("RJ", "33"), Map.entry("SP", "35"),
            Map.entry("PR", "41"), Map.entry("SC", "42"), Map.entry("RS", "43"), Map.entry("MS", "50"),
            Map.entry("MT", "51"), Map.entry("GO", "52"), Map.entry("DF", "53")
    );

    public SefazSoapEnvelopeProcessor(SefazProperties sefazProperties, CteXmlValidator cteXmlValidator) {
        this.sefazProperties = sefazProperties;
        this.cteXmlValidator = cteXmlValidator;
    }

    @Override
    public void process(Exchange exchange) {
        // 1. Resolução e validação rigorosa da UF e Código IBGE
        String uf = resolverUf(exchange);
        String cUF = resolverCodigoIbge(uf);

        // 2. Resolução do Ambiente e Versão do Schema CT-e
        CteAmbiente ambiente = resolverAmbiente(exchange);
        String tpAmb = ambiente.codigo();
        String versao = resolverVersaoCte();

        // 3. Armazena metadados estruturados no contexto da Exchange
        exchange.setProperty("SEFAZ_UF", uf);
        exchange.setProperty("SEFAZ_CUF", cUF);
        exchange.setProperty("SEFAZ_TP_AMB", tpAmb);
        exchange.setProperty("SEFAZ_AMBIENTE_NOME", ambiente.name());

        // 4. Montagem e higienização do XML interno (<consStatServCTe>)
        String innerXml = """
                <consStatServCTe versao="%s" xmlns="http://www.portalfiscal.inf.br/cte">
                  <tpAmb>%s</tpAmb>
                  <cUF>%s</cUF>
                  <xServ>STATUS</xServ>
                </consStatServCTe>
                """.formatted(versao, tpAmb, cUF).stripIndent().trim();

        // 5. Validação XSD prévia com contexto enriquecido
        try {
            cteXmlValidator.validarStatus(innerXml);
        } catch (Exception e) {
            log.error("[Camel SefazProcessor] Falha de validação XSD no XML gerado para UF {}: {}", uf, e.getMessage());
            throw new CteException("XML de consulta de status CT-e é inválido para a UF " + uf + ": " + e.getMessage(), e);
        }

        // 6. Construção e Codificação do Envelope SOAP 1.2
        String soapXml = SoapEnvelopeHelper.wrapCteSoap12(innerXml, CteSoapService.STATUS);
        String soapXmlBase64 = Base64Utils.encode(soapXml);

        exchange.setProperty("SEFAZ_XML_ENVIO_SOAP", soapXml);
        exchange.setProperty("SEFAZ_XML_ENVIO_SOAP_BASE64", soapXmlBase64);

        // 7. Configuração dos Headers HTTP para o componente HTTP do Apache Camel 3/4
        String actionHeader = CteSoapService.STATUS.soapAction();
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE,
                "application/soap+xml; charset=utf-8; action=\"" + actionHeader + "\"");
        exchange.getMessage().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getMessage().setBody(soapXml);

        log.info("[Camel SefazProcessor] Contexto SOAP 1.2 CT-e gerado com sucesso. UF: {} (IBGE: {}), Ambiente: {} (Payload: {} bytes)",
                uf, cUF, ambiente.name(), soapXml.length());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE RESOLUÇÃO DE PARÂMETROS
    // =========================================================================

    private String resolverUf(Exchange exchange) {
        // Tenta extrair a UF do DTO no Body
        Object body = exchange.getMessage().getBody();
        if (body instanceof CteStatusRequest request && request.getUf() != null && !request.getUf().isBlank()) {
            return request.getUf().trim().toUpperCase();
        }

        // Tenta extrair a UF dos Headers HTTP/Camel
        String headerUf = exchange.getMessage().getHeader("SEFAZ_UF", String.class);
        if (headerUf != null && !headerUf.isBlank()) {
            return headerUf.trim().toUpperCase();
        }

        // Fallback para a configuração padrão de estado da aplicação
        return Optional.ofNullable(sefazProperties.getEstado())
                .filter(s -> !s.isBlank())
                .map(s -> s.trim().toUpperCase())
                .orElse("MT");
    }

    private String resolverCodigoIbge(String uf) {
        String cUF = UF_IBGE_MAP.get(uf);
        if (cUF == null) {
            log.error("[Camel SefazProcessor] Tentativa de consulta para UF não suportada: '{}'", uf);
            throw new CteException("UF inválida ou não cadastrada na tabela do IBGE: " + uf);
        }
        return cUF;
    }

    private CteAmbiente resolverAmbiente(Exchange exchange) {
        Object body = exchange.getMessage().getBody();
        if (body instanceof CteStatusRequest request && request.getAmbiente() != null && !request.getAmbiente().isBlank()) {
            return CteAmbiente.from(request.getAmbiente());
        }

        String ambienteCfg = Optional.ofNullable(sefazProperties.getCte())
                .map(SefazProperties.Cte::getAmbiente)
                .filter(s -> !s.isBlank())
                .orElse("2");

        return CteAmbiente.from(ambienteCfg);
    }

    private String resolverVersaoCte() {
        return Optional.ofNullable(sefazProperties.getCte())
                .map(SefazProperties.Cte::getVersao)
                .filter(s -> !s.isBlank())
                .orElse("4.00");
    }
}
