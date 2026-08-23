package com.telemetria.integration.sefaz.cte.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;

import com.telemetria.integration.sefaz.cte.autorizacao.CteClient;
import com.telemetria.integration.sefaz.cte.route.CteRoute;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;

class CteApplicationServiceTests {

    @Test
    void encaminhaConsultaDeStatusParaARotaCamel() {
        ProducerTemplate producerTemplate = mock(ProducerTemplate.class);
        CteStatusResponse esperado = new CteStatusResponse("HOMOLOGACAO", "MT", true, "107", "ok", 1L);
        when(producerTemplate.requestBody(eq(CteRoute.ROUTE_CTE_STATUS), any(CteStatusRequest.class),
                eq(CteStatusResponse.class))).thenReturn(esperado);
        CteApplicationService service = new CteApplicationService(producerTemplate, mock(CteClient.class));

        CteStatusResponse atual = service.consultarStatus("MT", "2");

        assertThat(atual).isSameAs(esperado);
        verify(producerTemplate).requestBody(eq(CteRoute.ROUTE_CTE_STATUS), any(CteStatusRequest.class),
                eq(CteStatusResponse.class));
    }
}
