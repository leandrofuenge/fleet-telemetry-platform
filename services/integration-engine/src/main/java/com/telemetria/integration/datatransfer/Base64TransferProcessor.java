package com.telemetria.integration.datatransfer;

import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

@Component("base64TransferProcessor")
public class Base64TransferProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(Base64TransferProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Base64TransferRequest request = exchange.getIn().getBody(Base64TransferRequest.class);
        if (request == null) {
            request = new Base64TransferRequest();
        }

        String conteudoOriginal = request.getConteudo();

        // Se veio em Base64, decodifica
        if ((conteudoOriginal == null || conteudoOriginal.isBlank()) && request.getConteudoBase64() != null) {
            if (request.isCompactarGzip()) {
                conteudoOriginal = Base64Utils.decompressGzipBase64(request.getConteudoBase64());
            } else {
                conteudoOriginal = Base64Utils.decodeToString(request.getConteudoBase64());
            }
        }

        if (conteudoOriginal == null) {
            conteudoOriginal = "";
        }

        // Codifica para Base64
        String base64Gerado;
        if (request.isCompactarGzip()) {
            base64Gerado = Base64Utils.compressGzipBase64(conteudoOriginal);
        } else {
            base64Gerado = Base64Utils.encode(conteudoOriginal);
        }

        // Se solicitado encapsulamento em contexto SOAP 1.2
        String soapXml = null;
        String soapXmlBase64 = null;
        if (request.isEnveloparSoap()) {
            String tipo = (request.getTipoDocumento() != null) ? request.getTipoDocumento().toUpperCase() : "CTE";
            String msgTag = switch (tipo) {
                case "MDFE" -> "mdfeDadosMsg";
                case "NFE" -> "nfeDadosMsg";
                default -> "cteDadosMsg";
            };
            String namespace = switch (tipo) {
                case "MDFE" -> SoapEnvelopeHelper.DEFAULT_MDFE_NAMESPACE;
                case "NFE" -> SoapEnvelopeHelper.DEFAULT_NFE_NAMESPACE;
                default -> SoapEnvelopeHelper.DEFAULT_CTE_NAMESPACE;
            };

            soapXml = SoapEnvelopeHelper.wrapInSoap12(conteudoOriginal, msgTag, namespace);
            soapXmlBase64 = Base64Utils.encode(soapXml);
        }

        byte[] rawBytes = conteudoOriginal.getBytes(StandardCharsets.UTF_8);

        Base64TransferResponse response = new Base64TransferResponse();
        response.setSucesso(true);
        response.setTipoDocumento(request.getTipoDocumento());
        response.setConteudoOriginal(conteudoOriginal);
        response.setConteudoBase64(base64Gerado);
        response.setSoapEnvelopeXml(soapXml);
        response.setSoapEnvelopeXmlBase64(soapXmlBase64);
        response.setTamanhoBytesOriginal(rawBytes.length);
        response.setTamanhoBytesBase64(base64Gerado != null ? base64Gerado.length() : 0);
        response.setCompactadoGzip(request.isCompactarGzip());
        response.setMensagem("Transferência Base64 e contextualização SOAP processada com sucesso");

        log.info("[Camel Base64] Processado payload de {} bytes | Base64: {} chars | SOAP Envelopado: {}",
                rawBytes.length, response.getTamanhoBytesBase64(), request.isEnveloparSoap());

        exchange.getIn().setBody(response);
    }
}
