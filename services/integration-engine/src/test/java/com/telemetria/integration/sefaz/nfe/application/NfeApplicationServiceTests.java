package com.telemetria.integration.sefaz.nfe.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.sefaz.nfe.NfeBase64Codec;
import com.telemetria.integration.sefaz.nfe.NfeBase64Request;
import com.telemetria.integration.sefaz.nfe.NfeClient;
import com.telemetria.integration.sefaz.nfe.NfeProperties;

class NfeApplicationServiceTests {

    @Test
    void centralizaConsultaDeStatusNoCasoDeUso() {
        NfeClient client = org.mockito.Mockito.mock(NfeClient.class);
        when(client.consultarStatusServico()).thenReturn("<retConsStatServ versao=\"4.00\"/>");
        NfeApplicationService service = new NfeApplicationService(client, codec());

        assertThat(service.consultarStatusServico()).contains("retConsStatServ");
        verify(client).consultarStatusServico();
    }

    @Test
    void decodificaBase64AntesDeDelegarEEncodificaAResposta() {
        NfeClient client = org.mockito.Mockito.mock(NfeClient.class);
        String xml = "<distDFeInt versao=\"4.00\"/>";
        when(client.consultarDistribuicaoDfe(xml)).thenReturn("<retDistDFeInt versao=\"1.01\"/>");
        NfeApplicationService service = new NfeApplicationService(client, codec());

        var response = service.consultarDistribuicaoDfeBase64(new NfeBase64Request(
                Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8))));

        assertThat(new String(Base64.getDecoder().decode(response.xmlBase64()), StandardCharsets.UTF_8))
                .contains("retDistDFeInt");
        verify(client).consultarDistribuicaoDfe(xml);
    }

    private NfeBase64Codec codec() {
        NfeProperties properties = new NfeProperties();
        properties.setMaxXmlBytes(1024);
        return new NfeBase64Codec(properties);
    }
}
