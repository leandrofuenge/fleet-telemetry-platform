package com.telemetria.integration.nfe.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.telemetria.integration.nfe.domain.exception.NfeSefazUnavailableException;

class NfeHttpsSoapTransportTest {

    private static final URI ENDPOINT = URI.create("https://sefaz.example/nfe");

    @Test
    @SuppressWarnings("unchecked")
    void deveEnviarSoap12ComActionTimeoutEUtf8() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(headers("application/soap+xml; charset=utf-8"));
        when(response.body()).thenReturn(stream("<soap/>"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        when(client.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        NfeHttpsSoapTransport transport = new NfeHttpsSoapTransport(client, 1024);
        String result = transport.enviar(
                "<envelope>á</envelope>", ENDPOINT, NfeSoapService.STATUS, Duration.ofSeconds(5));

        HttpRequest request = requestCaptor.getValue();
        assertThat(result).isEqualTo("<soap/>");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.timeout()).contains(Duration.ofSeconds(5));
        assertThat(request.headers().firstValue("Content-Type")).hasValue(
                "application/soap+xml; charset=utf-8; action=\""
                        + NfeSoapService.STATUS.soapAction() + "\"");
        assertThat(request.headers().firstValue("Accept")).hasValue("application/soap+xml");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveRejeitarRespostaAcimaDoLimite() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(headers("application/soap+xml"));
        when(response.body()).thenReturn(stream("123456"));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        NfeHttpsSoapTransport transport = new NfeHttpsSoapTransport(client, 5);

        assertThatThrownBy(() -> transport.enviar(
                "<soap/>", ENDPOINT, NfeSoapService.STATUS, Duration.ofSeconds(1)))
                .isInstanceOf(NfeSefazUnavailableException.class)
                .hasMessageContaining("excede o limite");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveRejeitarStatusOuContentTypeInvalidos() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(503);
        when(response.body()).thenReturn(stream("indisponível"));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        NfeHttpsSoapTransport transport = new NfeHttpsSoapTransport(client, 1024);
        assertThatThrownBy(() -> transport.enviar(
                "<soap/>", ENDPOINT, NfeSoapService.STATUS, Duration.ofSeconds(1)))
                .isInstanceOf(NfeSefazUnavailableException.class)
                .hasMessageContaining("HTTP 503");

        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(headers("text/html"));
        when(response.body()).thenReturn(stream("<html/>"));
        assertThatThrownBy(() -> transport.enviar(
                "<soap/>", ENDPOINT, NfeSoapService.STATUS, Duration.ofSeconds(1)))
                .isInstanceOf(NfeSefazUnavailableException.class)
                .hasMessageContaining("Content-Type");
    }

    private HttpHeaders headers(String contentType) {
        return HttpHeaders.of(
                Map.of("Content-Type", List.of(contentType)),
                (name, value) -> true);
    }

    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
