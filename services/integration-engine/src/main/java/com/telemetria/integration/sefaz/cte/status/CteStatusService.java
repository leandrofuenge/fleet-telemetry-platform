package com.telemetria.integration.sefaz.cte.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.cte.CteAmbiente;

@Service("cteStatusService")
public class CteStatusService {

    private static final Logger log = LoggerFactory.getLogger(CteStatusService.class);
    private final SefazProperties sefazProperties;

    @Value("${integration.simulation.enabled:false}")
    private boolean simulationEnabled;

    public CteStatusService(SefazProperties sefazProperties) {
        this.sefazProperties = sefazProperties;
    }

    public CteStatusResponse consultar(CteStatusRequest request) {
        long inicio = System.currentTimeMillis();
        String uf = (request != null && request.getUf() != null) ? request.getUf() : "MT";
        String ambienteInformado = (request != null && request.getAmbiente() != null)
                ? request.getAmbiente() 
                : sefazProperties.getCte().getAmbiente();
        CteAmbiente ambiente = CteAmbiente.from(ambienteInformado);

        log.info("Consultando status SEFAZ CT-e para UF: {} no ambiente: {} via endpoint: {}", 
                uf, ambiente.nomeConfiguracao(), sefazProperties.getCte().getEndpoints().getStatus());

        long tempoResposta = System.currentTimeMillis() - inicio;

        CteStatusResponse response = new CteStatusResponse(
                ambiente.nomeConfiguracao(),
                uf,
                simulationEnabled,
                simulationEnabled ? "107" : "000",
                simulationEnabled
                        ? "Serviço em Operação (simulação explícita)"
                        : "Status não consultado: integração externa deve ser executada pela rota SEFAZ",
                tempoResposta
        );
        response.setSimulado(simulationEnabled);
        return response;
    }
}
