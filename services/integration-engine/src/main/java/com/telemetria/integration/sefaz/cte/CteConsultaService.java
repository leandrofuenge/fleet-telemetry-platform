package com.telemetria.integration.sefaz.cte;

import org.springframework.stereotype.Service;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.cte.infosimples.InfosimplesCteClient;
import com.telemetria.integration.sefaz.cte.infosimples.InfosimplesCteRequest;
import com.telemetria.integration.sefaz.cte.infosimples.InfosimplesCteResponse;

@Service
public class CteConsultaService {

    private final CteConsultaBuilder consultaBuilder;
    private final CteClient cteClient;
    private final InfosimplesCteClient infosimplesClient;
    private final SefazProperties sefazProperties;

    public CteConsultaService(CteConsultaBuilder consultaBuilder, 
                              CteClient cteClient, 
                              InfosimplesCteClient infosimplesClient,
                              SefazProperties sefazProperties) {
        this.consultaBuilder = consultaBuilder;
        this.cteClient = cteClient;
        this.infosimplesClient = infosimplesClient;
        this.sefazProperties = sefazProperties;
    }

    /**
     * Consulta SOAP leve diretamente na SEFAZ (Verifica apenas cStat/Situação).
     */
    public String consultarSituacaoSefaz(String chaveCte) {
        String xmlConsulta = consultaBuilder.buildXmlConsulta(
                chaveCte, sefazProperties.getCte().ambienteCte().codigo());
        return cteClient.consultarCte(xmlConsulta);
    }

    /**
     * Consulta REST enriquecida via Infosimples (Retorna XML completo, CIOT, RNTRC e motoristas).
     */
    public InfosimplesCteResponse consultarDadosCompletosInfosimples(String chaveCte, String certBase64, String senhaCert) {
        if (chaveCte == null || !chaveCte.matches("\\d{44}")) {
            throw new CteException("Chave de acesso do CT-e inválida para consulta.");
        }
        
        InfosimplesCteRequest request = new InfosimplesCteRequest(chaveCte, certBase64, senhaCert);
        return infosimplesClient.consultarCteCompleto(request);
    }
}
