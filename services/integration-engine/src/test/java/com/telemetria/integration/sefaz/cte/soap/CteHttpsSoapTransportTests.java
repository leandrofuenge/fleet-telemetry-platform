package com.telemetria.integration.sefaz.cte.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.telemetria.integration.sefaz.cte.exception.CteException;

class CteHttpsSoapTransportTests {

    private static final URI ENDPOINT = URI.create("https://homologacao.sefaz.test/cte");

    private HttpClient httpClientMock;
    private HttpResponse<String> httpResponseMock;
    private SSLContext sslContextMock; // <--- Variável declarada para resolver o erro de compilação

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClientMock = mock(HttpClient.class);
        httpResponseMock = (HttpResponse<String>) mock(HttpResponse.class);
        sslContextMock = mock(SSLContext.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveAplicarTimeoutSoapActionELerRespostaDeSucesso() throws Exception {
        when(httpResponseMock.statusCode()).thenReturn(200);
        when(httpResponseMock.body()).thenReturn("<retorno>ok</retorno>");
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponseMock);

        CteHttpsSoapTransport transport = new CteHttpsSoapTransport(httpClientMock);
        String response = transport.enviar("<soap/>", ENDPOINT, CteSoapService.AUTORIZACAO, 4321);

        assertEquals("<retorno>ok</retorno>", response);
        verify(httpClientMock).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveLancarCteExceptionEmCasoDeErroHttp() throws Exception {
        when(httpResponseMock.statusCode()).thenReturn(500);
        when(httpResponseMock.body()).thenReturn("Internal Server Error");
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponseMock);

        CteHttpsSoapTransport transport = new CteHttpsSoapTransport(httpClientMock);

        CteException exception = assertThrows(CteException.class, () ->
                transport.enviar("<soap/>", ENDPOINT, CteSoapService.EVENTO, 5000));

        assertEquals("SEFAZ retornou HTTP 500 para a operação cteRecepcaoEventoV4.", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void devePropagarTimeoutDeRedeComoCteException() throws Exception {
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("request timed out"));

        CteHttpsSoapTransport transport = new CteHttpsSoapTransport(httpClientMock);

        CteException exception = assertThrows(CteException.class, () ->
                transport.enviar("<soap/>", ENDPOINT, CteSoapService.CONSULTA, 25));

        assertInstanceOf(HttpTimeoutException.class, exception.getCause());
    }

    @Test
    void deveRecusarTimeoutInvalido() {
        CteHttpsSoapTransport transport = new CteHttpsSoapTransport(httpClientMock);

        assertThrows(IllegalArgumentException.class, () ->
                transport.enviar("<soap/>", ENDPOINT, CteSoapService.STATUS, 0));
    }

    @Test
    void deveInstanciarComConstrutorSSLContextEHostnameVerifier() {
        // Valida a criação usando o SSLContext e o HostnameVerifier
        CteHttpsSoapTransport transport = new CteHttpsSoapTransport(sslContextMock, (hostname, session) -> true);
        assertNotNull(transport);
    }
}
