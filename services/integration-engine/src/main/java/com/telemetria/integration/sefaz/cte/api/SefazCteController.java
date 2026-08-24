package com.telemetria.integration.sefaz.cte.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.sefaz.cte.application.CteApplicationService;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.retorno.CteAutorizacaoResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteConsultaResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteEventoResultado;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;

/**
 * Controller REST para disponibilizar os endpoints de integração com a SEFAZ (CT-e 4.00) via Apache Camel.
 */
@RestController
@RequestMapping("/api/integracoes/sefaz/cte")
public class SefazCteController {

    private static final Logger log = LoggerFactory.getLogger(SefazCteController.class);

    private final CteApplicationService applicationService;

    public SefazCteController(CteApplicationService applicationService) {
        this.applicationService = applicationService;
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

        CteStatusResponse response = applicationService.consultarStatus(ufNormalizada, ambiente);

        if (response == null) {
            log.error("Retorno nulo retornado da orquestração CT-e para a UF {}", ufNormalizada);
            throw new CteException("Falha ao obter resposta da integração SEFAZ. O serviço retornou uma resposta vazia.");
        }

        log.debug("Resposta do status CT-e recebida com sucesso para a UF: {}", ufNormalizada);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chaveAcesso}")
    public ResponseEntity<CteConsultaResultado> consultar(@PathVariable String chaveAcesso) {
        return ResponseEntity.ok(applicationService.consultar(chaveAcesso));
    }

    @PostMapping(value = "/autorizacoes", consumes = "application/xml")
    public ResponseEntity<CteAutorizacaoResultado> autorizar(@RequestBody String xmlCteAssinado) {
        return ResponseEntity.ok(applicationService.autorizar(xmlCteAssinado));
    }

    @PostMapping(value = "/eventos", consumes = "application/xml")
    public ResponseEntity<CteEventoResultado> enviarEvento(@RequestBody String xmlEventoAssinado) {
        return ResponseEntity.ok(applicationService.enviarEvento(xmlEventoAssinado));
    }
}
