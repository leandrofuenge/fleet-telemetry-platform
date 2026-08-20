package com.telemetria.integration.sefaz.cte;

import java.net.URI;

/**
 * Contrato para transporte mTLS (HTTPS com certificado digital A1)
 * utilizado nas chamadas aos WebServices SOAP da SEFAZ (CT-e 4.00).
 */
public interface CteSoapTransport {

    int DEFAULT_TIMEOUT_MILLIS = 5000;

    /**
     * Transmite uma requisição SOAP para a SEFAZ utilizando autenticação mTLS.
     *
     * @param soapRequest   Envelope XML SOAP a ser enviado
     * @param endpoint      URI do webservice da SEFAZ
     * @param service       Serviço SOAP de destino (ex: STATUS, AUTORIZACAO, EVENTO)
     * @param timeoutMillis Tempo limite de conexão e leitura em milissegundos
     * @return Resposta XML bruta devolvida pela SEFAZ
     * @throws CteException Em caso de falha na transmissão, erro mTLS ou timeout
     */
    String enviar(String soapRequest, URI endpoint, CteSoapService service, int timeoutMillis);

    /**
     * Transmite uma requisição SOAP utilizando o timeout padrão de 5 segundos.
     *
     * @param soapRequest Envelope XML SOAP a ser enviado
     * @param endpoint    URI do webservice da SEFAZ
     * @param service     Serviço SOAP de destino
     * @return Resposta XML bruta devolvida pela SEFAZ
     * @throws CteException Em caso de falha na transmissão, erro mTLS ou timeout
     */
    default String enviar(String soapRequest, URI endpoint, CteSoapService service) {
        return enviar(soapRequest, endpoint, service, DEFAULT_TIMEOUT_MILLIS);
    }
}