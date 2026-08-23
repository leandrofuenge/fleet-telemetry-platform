package com.telemetria.integration.sefaz.nfe.application;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.telemetria.integration.sefaz.nfe.NfeBase64Codec;
import com.telemetria.integration.sefaz.nfe.NfeBase64Request;
import com.telemetria.integration.sefaz.nfe.NfeBase64Response;
import com.telemetria.integration.sefaz.nfe.NfeClient;

/**
 * Casos de uso da NF-e.
 *
 * <p>Esta é a única camada chamada pela API HTTP. Ela mantém o protocolo de
 * entrada (XML ou Base64) fora do controller e delega as regras fiscais,
 * validações e a comunicação com a SEFAZ ao cliente especializado.</p>
 */
@Service
public class NfeApplicationService {

    private final NfeClient nfeClient;
    private final NfeBase64Codec base64Codec;

    public NfeApplicationService(NfeClient nfeClient, NfeBase64Codec base64Codec) {
        this.nfeClient = nfeClient;
        this.base64Codec = base64Codec;
    }

    public String consultarStatusServico() { return nfeClient.consultarStatusServico(); }
    public String consultarNfe(String chaveAcesso) { return nfeClient.consultarNfe(chaveAcesso); }
    public String consultarReciboAutorizacao(String numeroRecibo) {
        return nfeClient.consultarReciboAutorizacao(numeroRecibo);
    }
    public String autorizar(String xmlNfeAssinado) { return nfeClient.autorizarNfe(xmlNfeAssinado); }
    public String enviarEvento(String xmlEventoAssinado) { return nfeClient.enviarEvento(xmlEventoAssinado); }
    public String inutilizarNumeracao(String xmlInutilizacaoAssinado) {
        return nfeClient.inutilizarNumeracao(xmlInutilizacaoAssinado);
    }
    public String consultarDistribuicaoDfe(String xmlConsulta) {
        return nfeClient.consultarDistribuicaoDfe(xmlConsulta);
    }

    public NfeBase64Response autorizarBase64(NfeBase64Request request) {
        return processarBase64(request, nfeClient::autorizarNfe);
    }

    public NfeBase64Response enviarEventoBase64(NfeBase64Request request) {
        return processarBase64(request, nfeClient::enviarEvento);
    }

    public NfeBase64Response inutilizarNumeracaoBase64(NfeBase64Request request) {
        return processarBase64(request, nfeClient::inutilizarNumeracao);
    }

    public NfeBase64Response consultarDistribuicaoDfeBase64(NfeBase64Request request) {
        return processarBase64(request, nfeClient::consultarDistribuicaoDfe);
    }

    private NfeBase64Response processarBase64(NfeBase64Request request, Function<String, String> operacao) {
        String xmlResposta = operacao.apply(base64Codec.decodificarXml(request));
        return base64Codec.codificarResposta(xmlResposta);
    }
}
