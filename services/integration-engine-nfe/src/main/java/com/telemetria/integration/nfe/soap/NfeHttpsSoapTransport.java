package com.telemetria.integration.nfe.soap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.telemetria.integration.nfe.config.NfeProperties;
import com.telemetria.integration.nfe.domain.exception.NfeSefazUnavailableException;

/**
 * Transporte HTTP/1.1 para comunicação SOAP 1.2 com a SEFAZ NF-e,
 * utilizando mTLS através de um SSLContext configurado pelo Spring.
 *
 * Responsabilidade:
 * - estabelecer comunicação HTTPS;
 * - utilizar SSLContext/mTLS;
 * - enviar requisição SOAP;
 * - controlar timeout;
 * - validar resposta HTTP.
 *
 * Não é responsabilidade desta classe:
 * - interpretar XML da NF-e;
 * - processar SOAP Fault;
 * - aplicar regras de negócio;
 * - interpretar códigos de retorno da SEFAZ.
 */
@Component
public class NfeHttpsSoapTransport implements NfeSoapTransport {

    private static final String HTTPS_SCHEME = "https";

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String SOAP_MEDIA_TYPE = "application/soap+xml";

    private static final String SOAP_CONTENT_TYPE =
            "application/soap+xml; charset=utf-8; action=\"%s\"";

    private static final Duration CONNECT_TIMEOUT =
            Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final int maxResponseBytes;

    @Autowired
    public NfeHttpsSoapTransport(
            @Qualifier("sefazSslContext") SSLContext sslContext,
            NfeProperties properties) {

        if (sslContext == null) {
            throw new IllegalArgumentException(
                    "SSLContext da SEFAZ não pode ser nulo.");
        }
        if (properties == null) {
            throw new IllegalArgumentException(
                    "Configurações da NF-e não podem ser nulas.");
        }

        validarLimiteResposta(properties.getMaxXmlBytes());

        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.maxResponseBytes = properties.getMaxXmlBytes();
    }

    /**
     * Construtor utilizado para testes.
     */
    NfeHttpsSoapTransport(HttpClient httpClient, int maxResponseBytes) {

        if (httpClient == null) {
            throw new IllegalArgumentException(
                    "HttpClient não pode ser nulo.");
        }

        validarLimiteResposta(maxResponseBytes);
        this.httpClient = httpClient;
        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public String enviar(
            String envelopeSoap,
            URI endpoint,
            NfeSoapService service,
            Duration timeout) {

        validarParametros(
                envelopeSoap,
                endpoint,
                service,
                timeout);

        HttpRequest request = criarRequest(
                envelopeSoap,
                endpoint,
                service,
                timeout);

        try {

            HttpResponse<InputStream> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofInputStream());

            return processarResposta(
                    response,
                    service);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new NfeSefazUnavailableException(
                    "Comunicação com a SEFAZ NF-e foi interrompida para a operação "
                            + service.soapAction()
                            + ".",
                    exception);

        } catch (IOException exception) {

            throw new NfeSefazUnavailableException(
                    "Falha de comunicação HTTP com a SEFAZ NF-e para a operação "
                            + service.soapAction()
                            + ".",
                    exception);

        } catch (NfeSefazUnavailableException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            throw new NfeSefazUnavailableException(
                    "Falha inesperada na comunicação com a SEFAZ NF-e para a operação "
                            + service.soapAction()
                            + ".",
                    exception);
        }
    }

    private HttpRequest criarRequest(
            String envelopeSoap,
            URI endpoint,
            NfeSoapService service,
            Duration timeout) {

        String contentType =
                String.format(
                        SOAP_CONTENT_TYPE,
                        service.soapAction());

        return HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .header(CONTENT_TYPE, contentType)
                .header(ACCEPT, SOAP_MEDIA_TYPE)
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                envelopeSoap,
                                StandardCharsets.UTF_8))
                .build();
    }

    private String processarResposta(
            HttpResponse<InputStream> response,
            NfeSoapService service) throws IOException {

        int statusCode = response.statusCode();

        if (statusCode < 200 || statusCode >= 300) {

            fecharSilenciosamente(response.body());

            throw new NfeSefazUnavailableException(
                    "SEFAZ retornou HTTP "
                            + statusCode
                            + " para a operação "
                            + service.soapAction()
                            + ".");
        }

        String contentType = response.headers()
                .firstValue(CONTENT_TYPE)
                .orElse("");

        if (!contentType.toLowerCase(java.util.Locale.ROOT)
                .startsWith(SOAP_MEDIA_TYPE)) {
            fecharSilenciosamente(response.body());
            throw new NfeSefazUnavailableException(
                    "SEFAZ retornou Content-Type incompatível com SOAP 1.2 para a operação "
                            + service.soapAction()
                            + ".");
        }

        String body = lerCorpoLimitado(response.body(), service);

        if (body == null || body.isBlank()) {

            throw new NfeSefazUnavailableException(
                    "SEFAZ retornou HTTP "
                            + statusCode
                            + " sem corpo para a operação "
                            + service.soapAction()
                            + ".");
        }

        return body;
    }

    private String lerCorpoLimitado(
            InputStream body,
            NfeSoapService service) throws IOException {

        if (body == null) {
            return null;
        }

        try (InputStream input = body) {
            byte[] bytes = input.readNBytes(maxResponseBytes + 1);
            if (bytes.length > maxResponseBytes) {
                throw new NfeSefazUnavailableException(
                        "Resposta SOAP da SEFAZ excede o limite de "
                                + maxResponseBytes
                                + " bytes para a operação "
                                + service.soapAction()
                                + ".");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private void fecharSilenciosamente(InputStream body) {
        if (body == null) {
            return;
        }
        try {
            body.close();
        } catch (IOException ignored) {
            // A resposta já será rejeitada pelo Content-Type inválido.
        }
    }

    private static void validarLimiteResposta(int maxResponseBytes) {
        if (maxResponseBytes <= 0 || maxResponseBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Limite da resposta SOAP NF-e deve estar entre 1 e "
                            + (Integer.MAX_VALUE - 1)
                            + " bytes.");
        }
    }

    private void validarParametros(
            String envelopeSoap,
            URI endpoint,
            NfeSoapService service,
            Duration timeout) {

        if (envelopeSoap == null || envelopeSoap.isBlank()) {

            throw new IllegalArgumentException(
                    "Envelope SOAP da NF-e não pode ser vazio.");
        }

        if (endpoint == null) {

            throw new NfeSefazUnavailableException(
                    "Endpoint HTTPS NF-e não configurado.");
        }

        if (!HTTPS_SCHEME.equalsIgnoreCase(
                endpoint.getScheme())) {

            throw new NfeSefazUnavailableException(
                    "Endpoint da SEFAZ NF-e deve utilizar HTTPS.");
        }

        if (endpoint.getHost() == null
                || endpoint.getHost().isBlank()) {

            throw new NfeSefazUnavailableException(
                    "Host do endpoint da SEFAZ NF-e não configurado.");
        }

        if (service == null) {

            throw new IllegalArgumentException(
                    "Serviço SOAP da NF-e não pode ser nulo.");
        }

        if (service.soapAction() == null
                || service.soapAction().isBlank()) {

            throw new IllegalArgumentException(
                    "SOAP Action da operação NF-e não pode ser vazio.");
        }

        if (timeout == null
                || timeout.isZero()
                || timeout.isNegative()) {

            throw new IllegalArgumentException(
                    "Timeout NF-e deve ser maior que zero.");
        }
    }
}
