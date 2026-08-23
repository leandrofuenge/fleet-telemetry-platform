package com.telemetria.integration.sefaz.cte.application;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.telemetria.integration.sefaz.cte.autorizacao.CteClient;
import com.telemetria.integration.sefaz.cte.retorno.CteAutorizacaoResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteConsultaResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteEventoResultado;
import com.telemetria.integration.sefaz.cte.route.CteRoute;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;

/** Centraliza os casos de uso síncronos do CT-e sem expor Camel à API HTTP. */
@Service
public class CteApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CteApplicationService.class);

    private final ProducerTemplate producerTemplate;
    private final CteClient cteClient;

    public CteApplicationService(ProducerTemplate producerTemplate, CteClient cteClient) {
        this.producerTemplate = producerTemplate;
        this.cteClient = cteClient;
    }

    public CteStatusResponse consultarStatus(String uf, String ambiente) {
        log.info("CT-e: encaminhando consulta de status para a rota interna (uf={}, ambiente={})", uf, ambiente);
        CteStatusResponse response = producerTemplate.requestBody(
                CteRoute.ROUTE_CTE_STATUS, new CteStatusRequest(uf, ambiente), CteStatusResponse.class);
        log.info("CT-e: consulta de status concluída (uf={}, disponivel={}, simulado={})", uf,
                response != null && response.isDisponivel(), response != null && response.isSimulado());
        return response;
    }

    public CteAutorizacaoResultado autorizar(String xmlCteAssinado) {
        log.info("CT-e: encaminhando autorização para o cliente fiscal");
        return cteClient.autorizarCteComResultado(xmlCteAssinado);
    }

    public CteConsultaResultado consultar(String chaveAcesso) {
        log.info("CT-e: encaminhando consulta de documento para o cliente fiscal");
        return cteClient.consultarCteComResultado(chaveAcesso);
    }

    public CteEventoResultado enviarEvento(String xmlEventoAssinado) {
        log.info("CT-e: encaminhando evento para o cliente fiscal");
        return cteClient.enviarEventoComResultado(xmlEventoAssinado);
    }
}
