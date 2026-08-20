package com.telemetria.integration.sefaz.cte;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;

/**
 * Controller REST para disponibilizar os endpoints de integração com a SEFAZ (CT-e 4.00) via Apache Camel.
 */
@RestController
@RequestMapping("/api/integracoes/sefaz/cte")
public class SefazCteController {

    private static final Logger log = LoggerFactory.getLogger(SefazCteController.class);

    private final ProducerTemplate producerTemplate;

    public SefazCteController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    /**
     * Consulta a disponibilidade do serviço de autorização do CT-e na SEFAZ.
     *
     * @param uf       UF do webservice de destino (ex: MT, SP, RS). Padrão: MT
     * @param ambiente Código do ambiente (1 = Produção, 2 = Homologação - Opcional)
     * @return Status do serviço retornado pela SEFAZ
     */
    @GetMapping("/status")
    public ResponseEntity<CteStatusResponse> consultarStatus(
            @RequestParam(required = false, defaultValue = "MT") String uf,
            @RequestParam(required = false) String ambiente) {

        String ufNormalizada = (uf != null) ? uf.trim().toUpperCase() : "MT";

        if (ufNormalizada.length() != 2) {
            throw new CteException("Sigla da UF inválida. Deve conter exatamente 2 caracteres (ex: MT, SP, RJ).");
        }

        log.info("Recebida requisição REST de consulta de status CT-e. UF: {}, Ambiente: {}", ufNormalizada, ambiente);

        CteStatusRequest request = new CteStatusRequest(ufNormalizada, ambiente);

        CteStatusResponse response = producerTemplate.requestBody(
                CteRoute.ROUTE_CTE_STATUS,
                request,
                CteStatusResponse.class
        );

        if (response == null) {
            log.error("Retorno nulo retornado da rota Camel '{}' para a UF {}", CteRoute.ROUTE_CTE_STATUS, ufNormalizada);
            throw new CteException("Falha ao obter resposta da integração SEFAZ. O serviço retornou uma resposta vazia.");
        }

        log.debug("Resposta do status CT-e recebida com sucesso para a UF: {}", ufNormalizada);
        return ResponseEntity.ok(response);
    }
}