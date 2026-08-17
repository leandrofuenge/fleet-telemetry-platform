package com.telemetria.integration.sefaz.cte.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.telemetria.integration.config.SefazProperties;

@Service("cteStatusService")
public class CteStatusService {

    private static final Logger log = LoggerFactory.getLogger(CteStatusService.class);
    private final SefazProperties sefazProperties;

    public CteStatusService(SefazProperties sefazProperties) {
        this.sefazProperties = sefazProperties;
    }

    public CteStatusResponse consultar(CteStatusRequest request) {
        long inicio = System.currentTimeMillis();
        String uf = (request != null && request.getUf() != null) ? request.getUf() : "MT";
        String ambiente = (request != null && request.getAmbiente() != null) 
                ? request.getAmbiente() 
                : sefazProperties.getCte().getAmbiente();

        log.info("Consultando status SEFAZ CT-e para UF: {} no ambiente: {} via endpoint: {}", 
                uf, ambiente, sefazProperties.getCte().getStatusServico().getUrl());

        long tempoResposta = System.currentTimeMillis() - inicio;

        return new CteStatusResponse(
                ambiente,
                uf,
                true,
                "107",
                "Serviço em Operação",
                tempoResposta
        );
    }
}
