package com.telemetria.integration.nfe.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.integration.nfe.application.NfeApplicationService;
import com.telemetria.integration.nfe.application.dto.NfeBase64Request;
import com.telemetria.integration.nfe.application.dto.NfeBase64Response;

/** API interna da NF-e. Operações mutáveis permanecem bloqueadas por padrão. */
@RestController
@RequestMapping("/api/integracoes/sefaz/nfe")
public class NfeController {
    private final NfeApplicationService applicationService;

    public NfeController(NfeApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> status() { return ResponseEntity.ok(applicationService.consultarStatusServico()); }

    @GetMapping(value = "/{chaveAcesso}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> consultar(@PathVariable String chaveAcesso) {
        return ResponseEntity.ok(applicationService.consultarNfe(chaveAcesso));
    }

    @GetMapping(value = "/recibos/{numero}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> consultarRecibo(@PathVariable String numero) {
        return ResponseEntity.ok(applicationService.consultarReciboAutorizacao(numero));
    }

    @PostMapping(value = "/autorizacoes", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> autorizar(@RequestBody String xmlNfeAssinado) {
        return ResponseEntity.ok(applicationService.autorizar(xmlNfeAssinado));
    }

    @PostMapping(value = "/autorizacoes/base64", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NfeBase64Response> autorizarBase64(@RequestBody NfeBase64Request request) {
        return ResponseEntity.ok(applicationService.autorizarBase64(request));
    }

    @PostMapping(value = "/eventos", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> enviarEvento(@RequestBody String xmlEventoAssinado) {
        return ResponseEntity.ok(applicationService.enviarEvento(xmlEventoAssinado));
    }

    @PostMapping(value = "/eventos/base64", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NfeBase64Response> enviarEventoBase64(@RequestBody NfeBase64Request request) {
        return ResponseEntity.ok(applicationService.enviarEventoBase64(request));
    }

    @PostMapping(value = "/inutilizacoes", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> inutilizar(@RequestBody String xmlInutAssinado) {
        return ResponseEntity.ok(applicationService.inutilizarNumeracao(xmlInutAssinado));
    }

    @PostMapping(value = "/inutilizacoes/base64", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NfeBase64Response> inutilizarBase64(@RequestBody NfeBase64Request request) {
        return ResponseEntity.ok(applicationService.inutilizarNumeracaoBase64(request));
    }

    @PostMapping(value = "/distribuicao-dfe", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> distribuir(@RequestBody String xmlConsulta,
            @RequestParam(defaultValue = "false") boolean confirmarConsulta) {
        if (!confirmarConsulta) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(applicationService.consultarDistribuicaoDfe(xmlConsulta));
    }

    @PostMapping(value = "/distribuicao-dfe/base64", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NfeBase64Response> distribuirBase64(@RequestBody NfeBase64Request request,
            @RequestParam(defaultValue = "false") boolean confirmarConsulta) {
        if (!confirmarConsulta) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(applicationService.consultarDistribuicaoDfeBase64(request));
    }

}
