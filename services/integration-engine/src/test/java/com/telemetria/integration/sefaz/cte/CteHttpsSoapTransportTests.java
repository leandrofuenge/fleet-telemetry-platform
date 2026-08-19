package com.telemetria.integration.sefaz.cte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Test;

class CteHttpsSoapTransportTests {

    private static final URI ENDPOINT = URI.create("https://homologacao.sefaz.test/cte");

    @Test
    void deveAplicarTimeoutSoapActionELerRespostaDeSucesso() throws Exception {
        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        when(connection.getOutputStream()).thenReturn(sent);
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getInputStream()).thenReturn(stream("<retorno>ok</retorno>"));

        CteHttpsSoapTransport transport = transport(connection);
        String response = transport.enviar("<soap/>", ENDPOINT, CteSoapService.AUTORIZACAO, 4321);

        assertEquals("<retorno>ok</retorno>", response);
        assertEquals("<soap/>", sent.toString(StandardCharsets.UTF_8));
        verify(connection).setConnectTimeout(4321);
        verify(connection).setReadTimeout(4321);
        verify(connection).setRequestProperty("Content-Type",
                "application/soap+xml; charset=utf-8; action=\"" +
                        CteSoapService.AUTORIZACAO.soapAction() + "\"");
    }

    @Test
    void devePreservarCorpoDeRejeicaoHttpParaParserDaOperacao() throws Exception {
        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(connection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(connection.getResponseCode()).thenReturn(500);
        when(connection.getErrorStream()).thenReturn(stream("<soap:Fault/>"));

        String response = transport(connection).enviar(
                "<soap/>", ENDPOINT, CteSoapService.EVENTO, 5000);

        assertEquals("<soap:Fault/>", response);
    }

    @Test
    void devePropagarTimeoutSemRealizarAcessoDeRede() throws Exception {
        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(connection.getOutputStream()).thenThrow(new SocketTimeoutException("read timed out"));

        CteException exception = assertThrows(CteException.class, () -> transport(connection).enviar(
                "<soap/>", ENDPOINT, CteSoapService.CONSULTA, 25));

        assertInstanceOf(SocketTimeoutException.class, exception.getCause());
        verify(connection).setConnectTimeout(25);
        verify(connection).setReadTimeout(25);
    }

    @Test
    void deveRecusarTimeoutInvalido() {
        assertThrows(IllegalArgumentException.class, () -> transport(mock(HttpsURLConnection.class)).enviar(
                "<soap/>", ENDPOINT, CteSoapService.STATUS, 0));
    }

    private CteHttpsSoapTransport transport(HttpsURLConnection connection) throws Exception {
        return new CteHttpsSoapTransport(SSLContext.getDefault(), ignored -> connection);
    }

    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
