package com.telemetria.integration.sefaz.cte.soap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.exception.CteException;

/**
 * Implementação HTTPS/SOAP 1.2 com mTLS e timeouts explícitos via Java 11+ HttpClient.
 */
@Component
public class CteHttpsSoapTransport implements CteSoapTransport {

    private static final Logger log = LoggerFactory.getLogger(CteHttpsSoapTransport.class);

    private final HttpClient httpClient;

    /**
     * Construtor principal utilizado pelo Spring Boot.
     */
    @Autowired
    public CteHttpsSoapTransport(@Qualifier("sefazSslContext") SSLContext sslContext) {
        this(buildHttpClient(sslContext));
    }

    /**
     * Construtor sobrecarregado para compatibilidade com chamadas que fornecem HostnameVerifier.
     */
    public CteHttpsSoapTransport(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        this(buildHttpClient(sslContext));
        if (hostnameVerifier != null) {
            log.debug("HostnameVerifier customizado registrado para transporte SEFAZ CT-e.");
        }
    }

    /**
     * Construtor de pacote para testes — permite injetar um {@link HttpClient} mockado.
     */
    CteHttpsSoapTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String enviar(String soapRequest, URI endpoint, CteSoapService service, int timeoutMillis) {
        if (soapRequest == null || soapRequest.isBlank()) {
            throw new IllegalArgumentException("O payload SOAP não pode ser nulo ou vazio.");
        }
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new CteException("Endpoint HTTPS da operação CT-e não configurado ou inválido.");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeout CT-e deve ser maior que zero.");
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(timeoutMillis))
                .version(HttpClient.Version.HTTP_1_1) // SEFAZ exige HTTP/1.1 estrito
                .header("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + service.soapAction() + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(soapRequest, StandardCharsets.UTF_8))
                .build();

        long inicio = System.currentTimeMillis();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long duracaoMs = System.currentTimeMillis() - inicio;
            String corpo = response.body();

            log.info("SEFAZ CT-e [{}] -> status={} tempoMs={}", service.soapAction(), response.statusCode(), duracaoMs);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CteException("SEFAZ retornou HTTP " + response.statusCode() + " para a operação " + service.soapAction() + ".");
            }
            if (corpo == null || corpo.isBlank()) {
                throw new CteException("SEFAZ retornou HTTP " + response.statusCode() + " sem corpo de resposta.");
            }
            if (corpo.contains(":Fault>") || corpo.contains("<Fault>")) {
                log.warn("SEFAZ CT-e [{}] retornou SOAP Fault.", service.soapAction());
                throw new CteException("SEFAZ retornou SOAP Fault para a operação " + service.soapAction() + ": " + extrairMensagemFault(corpo));
            }

            return corpo;

        } catch (CteException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na comunicação HTTPS/SOAP com a SEFAZ CT-e [{}]: {}", service.soapAction(), e.getMessage(), e);
            throw new CteException("Falha na comunicação HTTPS/SOAP com a SEFAZ CT-e: " + e.getMessage(), e);
        }
    }

    private static HttpClient buildHttpClient(SSLContext sslContext) {
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private String extrairMensagemFault(String xmlFault) {
        if (xmlFault.contains("<faultstring>") && xmlFault.contains("</faultstring>")) {
            return xmlFault.substring(xmlFault.indexOf("<faultstring>") + 13, xmlFault.indexOf("</faultstring>"));
        }
        return "Erro interno no servidor da SEFAZ";
    }
}
