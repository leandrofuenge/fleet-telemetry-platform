package com.telemetria.integration.sefaz.cte;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Implementação HTTPS/SOAP 1.2 com mTLS e timeouts explícitos. */
@Component
public class CteHttpsSoapTransport implements CteSoapTransport {

    private final SSLContext sslContext;
    private final Function<URI, HttpsURLConnection> connectionFactory;

    @Autowired
    public CteHttpsSoapTransport(@Qualifier("sefazSslContext") SSLContext sslContext) {
        this(sslContext, CteHttpsSoapTransport::abrirConexao);
    }

    CteHttpsSoapTransport(SSLContext sslContext,
            Function<URI, HttpsURLConnection> connectionFactory) {
        this.sslContext = sslContext;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public String enviar(String soapRequest, URI endpoint, CteSoapService service, int timeoutMillis) {
        if (soapRequest == null || soapRequest.isBlank()) {
            throw new IllegalArgumentException("O payload SOAP não pode ser nulo ou vazio.");
        }
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new CteException("Endpoint HTTPS da operação CT-e não configurado.");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeout CT-e deve ser maior que zero.");
        }

        try {
            HttpsURLConnection connection = connectionFactory.apply(endpoint);
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setRequestProperty("Content-Type",
                    "application/soap+xml; charset=utf-8; action=\"" + service.soapAction() + "\"");

            byte[] postData = soapRequest.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(postData.length));
            try (OutputStream output = connection.getOutputStream()) {
                output.write(postData);
            }

            int responseCode = connection.getResponseCode();
            InputStream input = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            if (input == null) {
                throw new CteException("SEFAZ retornou HTTP " + responseCode + " sem corpo.");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (CteException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Falha na comunicação HTTPS/SOAP com a SEFAZ: " + e.getMessage(), e);
        }
    }

    private static HttpsURLConnection abrirConexao(URI endpoint) {
        try {
            return (HttpsURLConnection) endpoint.toURL().openConnection();
        } catch (Exception e) {
            throw new CteException("Não foi possível abrir o endpoint CT-e.", e);
        }
    }
}
