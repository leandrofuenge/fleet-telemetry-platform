package com.telemetria.integration.nfe.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.config.NfeProperties;

class NfeSoapGatewayTest {

    @Test
    void deveOrquestrarEnvelopeTransporteEValidacao() {
        NfeSoapEnvelopeFactory envelopeFactory = mock(NfeSoapEnvelopeFactory.class);
        NfeSoapTransport transport = mock(NfeSoapTransport.class);
        NfeSoapResponseValidator validator = mock(NfeSoapResponseValidator.class);
        NfeProperties properties = new NfeProperties();
        properties.setTimeoutMillis(2500);
        URI endpoint = URI.create("https://sefaz.example/nfe");
        when(envelopeFactory.criar(NfeSoapService.STATUS, "<consulta/>")).thenReturn("<soap/>");
        when(transport.enviar(
                "<soap/>", endpoint, NfeSoapService.STATUS, Duration.ofMillis(2500)))
                .thenReturn("<resposta/>");

        NfeSoapGateway gateway = new NfeSoapGateway(
                envelopeFactory, transport, validator, properties);

        assertThat(gateway.enviar(NfeSoapService.STATUS, endpoint, "<consulta/>"))
                .isEqualTo("<resposta/>");
        verify(validator).validar("<resposta/>", NfeSoapService.STATUS);
    }

    @Test
    void deveRejeitarEndpointInseguroETempoInvalido() {
        NfeProperties properties = new NfeProperties();
        NfeSoapGateway gateway = new NfeSoapGateway(
                mock(NfeSoapEnvelopeFactory.class),
                mock(NfeSoapTransport.class),
                mock(NfeSoapResponseValidator.class),
                properties);

        assertThatThrownBy(() -> gateway.enviar(
                NfeSoapService.STATUS, URI.create("http://sefaz.example"), "<xml/>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");

        properties.setTimeoutMillis(0);
        assertThatThrownBy(() -> gateway.enviar(
                NfeSoapService.STATUS, URI.create("https://sefaz.example"), "<xml/>"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Timeout");
    }
}
