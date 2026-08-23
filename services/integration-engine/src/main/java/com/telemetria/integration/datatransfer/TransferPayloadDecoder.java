package com.telemetria.integration.datatransfer;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.telemetria.integration.util.Base64Utils;

/**
 * Resolve o conteúdo de entrada, seja ele texto puro, Base64 ou Base64 compactado com GZIP.
 */
@Component
public class TransferPayloadDecoder {

    private final DocumentoFiscalXmlValidator documentoFiscalXmlValidator;

    public TransferPayloadDecoder(DocumentoFiscalXmlValidator documentoFiscalXmlValidator) {
        this.documentoFiscalXmlValidator = documentoFiscalXmlValidator;
    }

    public String decodificar(Base64TransferRequest request) throws IOException {
        String conteudo = request.getConteudo();
        if ((conteudo == null || conteudo.isBlank()) && request.getConteudoBase64() != null) {
            validarTamanhoBase64(request.getConteudoBase64());
            conteudo = request.isEntradaCompactadaGzip()
                    ? Base64Utils.decompressGzipBase64(
                            request.getConteudoBase64(), documentoFiscalXmlValidator.getMaxDocumentBytes())
                    : Base64Utils.decodeToString(request.getConteudoBase64());
        }
        return conteudo == null ? "" : conteudo;
    }

    private void validarTamanhoBase64(String base64) {
        int maxCaracteres = ((documentoFiscalXmlValidator.getMaxDocumentBytes() + 2) / 3) * 4;
        if (base64.length() > maxCaracteres) {
            throw new DataTransferValidationException(
                    "Conteúdo Base64 excede o limite permitido de " + maxCaracteres + " caracteres.");
        }
    }
}
