package com.telemetria.integration.sefaz.cte;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CteConsultaService {

    private final CteConsultaBuilder consultaBuilder;
    private final CteClient cteClient;

    @Value("${sefaz.ambiente:2}") // 1=Produção, 2=Homologação
    private String tpAmb;

    public CteConsultaService(CteConsultaBuilder consultaBuilder, CteClient cteClient) {
        this.consultaBuilder = consultaBuilder;
        this.cteClient = cteClient;
    }

    /**
     * Realiza a consulta da situação de um CT-e na SEFAZ.
     *
     * @param chaveCte Chave de acesso de 44 dígitos
     * @return XML contendo a resposta da SEFAZ (<retConsSitCTe>)
     */
    public String consultarCte(String chaveCte) {
        // 1. Monta o XML da consulta
        String xmlConsulta = consultaBuilder.buildXmlConsulta(chaveCte, tpAmb);

        // 2. Envia para o WebService via CteClient (sem necessidade de assinatura digital)
        return cteClient.consultarCte(xmlConsulta);
    }
    
 // Rota para consulta de CT-e via Chave de Acesso
    from("direct:consultarCte")
        .routeId("rota-consulta-cte")
        .log("Consultando situação do CT-e chave: ${body}")
        .bean("cteConsultaService", "consultarCte")
        .process("cteResponseParserProcessor") // Trata o XML de retorno para objeto Java (CteResultadoParse)
        .to("log:resultadoConsultaCte?level=INFO");
}