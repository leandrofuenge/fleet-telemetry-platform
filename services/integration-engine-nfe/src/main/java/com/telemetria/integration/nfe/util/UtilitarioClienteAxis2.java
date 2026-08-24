package com.telemetria.integration.nfe.util;

import org.apache.axis2.client.Stub;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.commons.httpclient.HttpClient;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;

public class UtilitarioClienteAxis2 {

    private UtilitarioClienteAxis2() {
    }

    /**
     *
     * Configura o stub para usar certificado e HttpClient específicos, desde que esteja com
     * configuracoesNfe.getCertificado().isModoMultithreading() ativo.
     *
     * @param stub {@link org.apache.axis2.client.Stub} que terá a conexão configurada para o {@link org.apache.commons.httpclient.HttpClient}
     *             específico do certificado.
     * @param configuracoesNfe {@link com.telemetria.integration.nfe.dom.ConfiguracoesNfe} config que possui o {@link br.com.swconsultoria.certificado.Certificado}
     *                         que será configurado no HttpClient.
     * @param url {@link String} url que será executada.
     * @throws CertificadoException
     */
    public static void configuraHttpClient(Stub stub, ConfiguracoesNfe configuracoesNfe, String url) throws CertificadoException {
        Certificado certificado = configuracoesNfe.getCertificado();
        if (certificado.isModoMultithreading()) {
            HttpClient httpClient = ObjetoUtil.verifica(configuracoesNfe.getCacert()).isPresent()
                    ? CertificadoService.getHttpsClient(certificado, url, configuracoesNfe.getCacert())
                    : CertificadoService.getHttpsClient(certificado, url);

            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpClient);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CUSTOM_PROTOCOL_HANDLER, httpClient.getHostConfiguration().getProtocol());
        }
    }
}

