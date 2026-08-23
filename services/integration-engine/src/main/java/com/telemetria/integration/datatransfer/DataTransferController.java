package com.telemetria.integration.datatransfer;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.util.Base32Utils;
import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

@RestController
@RequestMapping("/api/integracoes/transfer")
public class DataTransferController {

    private final OrquestradorTransferenciaDados orquestradorTransferenciaDados;

    public DataTransferController(OrquestradorTransferenciaDados orquestradorTransferenciaDados) {
        this.orquestradorTransferenciaDados = orquestradorTransferenciaDados;
    }

    /**
     * Processa a transferência de dados e documentos (XML/JSON/binário) com suporte a Base64 e contextualização SOAP.
     */
    @PostMapping(value = "/base64", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Base64TransferResponse> processarTransferenciaBase64(
            @RequestBody Base64TransferRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        try {
            OrquestradorTransferenciaDados.ResultadoTransferenciaDados resultado =
                    orquestradorTransferenciaDados.processar(request, correlationId);
            return ResponseEntity.ok()
                    .header("X-Correlation-ID", resultado.correlationId())
                    .body(resultado.response());
        } catch (DataTransferValidationException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
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

    /** Utilitário para códigos de dispositivos, identificadores e segredos compatíveis com Base32. */
    @PostMapping(value = "/base32/encode", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> encodeBase32(@RequestBody String texto) {
        return ResponseEntity.ok(Map.of("original", texto, "base32", Base32Utils.encode(texto)));
    }

    /** Decodifica Base32 RFC 4648; espaços, hífens, preenchimento e minúsculas são aceitos. */
    @PostMapping(value = "/base32/decode", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> decodeBase32(@RequestBody String base32) {
        return ResponseEntity.ok(Map.of("base32", base32, "original", Base32Utils.decodeToString(base32)));
    }
}
