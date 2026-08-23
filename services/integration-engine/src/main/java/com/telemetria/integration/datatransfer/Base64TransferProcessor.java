package com.telemetria.integration.datatransfer;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component("base64TransferProcessor")
public class Base64TransferProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(Base64TransferProcessor.class);
    private final TransferPayloadDecoder payloadDecoder;
    private final DocumentoFiscalXmlValidator documentoFiscalXmlValidator;
    private final TransferResponseFactory responseFactory;

    public Base64TransferProcessor(
            TransferPayloadDecoder payloadDecoder,
            DocumentoFiscalXmlValidator documentoFiscalXmlValidator,
            TransferResponseFactory responseFactory) {
        this.payloadDecoder = payloadDecoder;
        this.documentoFiscalXmlValidator = documentoFiscalXmlValidator;
        this.responseFactory = responseFactory;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Base64TransferRequest request = exchange.getIn().getBody(Base64TransferRequest.class);
        if (request == null) {
            request = new Base64TransferRequest();
        }

        String conteudoOriginal = payloadDecoder.decodificar(request);

        if (request.isValidarDocumentoXml()) {
            documentoFiscalXmlValidator.validar(conteudoOriginal, request.getTipoDocumento());
        }

        Base64TransferResponse response = responseFactory.criar(request, conteudoOriginal);

        log.info("[Camel Base64] Processado payload de {} bytes | Base64: {} chars | SOAP Envelopado: {}",
                response.getTamanhoBytesOriginal(), response.getTamanhoBytesBase64(), request.isEnveloparSoap());

        exchange.getIn().setBody(response);
    }
}
