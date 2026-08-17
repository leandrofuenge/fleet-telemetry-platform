package com.telemetria.integration.controller;

import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.model.Base64TransferRequest;
import com.telemetria.integration.model.Base64TransferResponse;
import com.telemetria.integration.route.DataTransferRoute;
import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

@RestController
@RequestMapping("/api/integracoes/transfer")
public class DataTransferController {

    private final ProducerTemplate producerTemplate;

    public DataTransferController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    /**
     * Processa a transferência de dados e documentos (XML/JSON/binário) com suporte a Base64 e contextualização SOAP.
     */
    @PostMapping(value = "/base64", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Base64TransferResponse> processarTransferenciaBase64(@RequestBody Base64TransferRequest request) {
        Base64TransferResponse response = producerTemplate.requestBody(
                DataTransferRoute.ROUTE_TRANSFER_BASE64,
                request,
                Base64TransferResponse.class
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Envelopa diretamente um XML interno em um contexto SOAP 1.2.
     */
    @PostMapping(value = "/soap/wrap", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> enveloparSoap(
            @RequestBody String innerXml,
            @RequestParam(required = false, defaultValue = "CTE") String tipoDocumento) {

        String msgTag = "MDFE".equalsIgnoreCase(tipoDocumento) ? "mdfeDadosMsg" : "cteDadosMsg";
        String namespace = "MDFE".equalsIgnoreCase(tipoDocumento) 
                ? SoapEnvelopeHelper.DEFAULT_MDFE_NAMESPACE 
                : SoapEnvelopeHelper.DEFAULT_CTE_NAMESPACE;

        String soapXml = SoapEnvelopeHelper.wrapInSoap12(innerXml, msgTag, namespace);
        return ResponseEntity.ok(soapXml);
    }

    /**
     * Extrai o XML de negócio de dentro de um envelope SOAP 1.2.
     */
    @PostMapping(value = "/soap/unwrap", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> desmembrarSoap(@RequestBody String soapXml) {
        String innerXml = SoapEnvelopeHelper.extractInnerXml(soapXml);
        return ResponseEntity.ok(innerXml);
    }

    /**
     * Utilitário rápido de conversão Texto/XML para Base64.
     */
    @PostMapping(value = "/encode", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> encodeBase64(@RequestBody String texto) {
        String base64 = Base64Utils.encode(texto);
        return ResponseEntity.ok(Map.of(
                "original", texto,
                "base64", base64
        ));
    }

    /**
     * Utilitário rápido de decodificação Base64 para Texto/XML.
     */
    @PostMapping(value = "/decode", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> decodeBase64(@RequestBody String base64) {
        String texto = Base64Utils.decodeToString(base64);
        return ResponseEntity.ok(Map.of(
                "base64", base64,
                "original", texto
        ));
    }
}
