package com.telemetria.integration.datatransfer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.telemetria.integration.util.Base64Utils;
import com.telemetria.integration.util.SoapEnvelopeHelper;

/** Monta a representação de saída sem alterar o conteúdo recebido. */
@Component
public class TransferResponseFactory {

    public Base64TransferResponse criar(Base64TransferRequest request, String conteudoOriginal) throws IOException {
        String base64Gerado = request.isCompactarRespostaGzip()
                ? Base64Utils.compressGzipBase64(conteudoOriginal)
                : Base64Utils.encode(conteudoOriginal);

        String soapXml = null;
        String soapXmlBase64 = null;
        if (request.isEnveloparSoap()) {
            String tipo = request.getTipoDocumento() == null ? "CTE" : request.getTipoDocumento().toUpperCase();
            soapXml = SoapEnvelopeHelper.wrapInSoap12(conteudoOriginal, tagMensagem(tipo), namespace(tipo));
            soapXmlBase64 = Base64Utils.encode(soapXml);
        }

        Base64TransferResponse response = new Base64TransferResponse();
        response.setSucesso(true);
        response.setTipoDocumento(request.getTipoDocumento());
        if (request.isIncluirConteudoNaResposta()) {
            response.setConteudoOriginal(conteudoOriginal);
            response.setConteudoBase64(base64Gerado);
            response.setSoapEnvelopeXml(soapXml);
            response.setSoapEnvelopeXmlBase64(soapXmlBase64);
        }
        response.setTamanhoBytesOriginal(conteudoOriginal.getBytes(StandardCharsets.UTF_8).length);
        response.setTamanhoBytesBase64(base64Gerado.length());
        response.setCompactadoGzip(request.isCompactarRespostaGzip());
        response.setEntradaCompactadaGzip(request.isEntradaCompactadaGzip());
        response.setRespostaCompactadaGzip(request.isCompactarRespostaGzip());
        response.setMensagem("Transferência Base64 e contextualização SOAP processada com sucesso");
        return response;
    }

    private String tagMensagem(String tipo) {
        return switch (tipo) {
            case "MDFE" -> "mdfeDadosMsg";
            case "NFE" -> "nfeDadosMsg";
            default -> "cteDadosMsg";
        };
    }

    private String namespace(String tipo) {
        return switch (tipo) {
            case "MDFE" -> SoapEnvelopeHelper.DEFAULT_MDFE_NAMESPACE;
            case "NFE" -> SoapEnvelopeHelper.DEFAULT_NFE_NAMESPACE;
            default -> SoapEnvelopeHelper.DEFAULT_CTE_NAMESPACE;
        };
    }
}
