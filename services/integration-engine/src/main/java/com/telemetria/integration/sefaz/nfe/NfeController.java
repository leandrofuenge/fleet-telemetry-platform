package com.telemetria.integration.sefaz.nfe;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API interna da NF-e. Operações mutáveis permanecem bloqueadas por padrão. */
@RestController
@RequestMapping("/api/integracoes/sefaz/nfe")
public class NfeController {
    private final NfeClient client;

    public NfeController(NfeClient client) { this.client = client; }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> status() { return ResponseEntity.ok(client.consultarStatusServico()); }

    @GetMapping(value = "/{chaveAcesso}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> consultar(@PathVariable String chaveAcesso) {
        return ResponseEntity.ok(client.consultarNfe(chaveAcesso));
    }

    @GetMapping(value = "/recibos/{numero}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> consultarRecibo(@PathVariable String numero) {
        return ResponseEntity.ok(client.consultarReciboAutorizacao(numero));
    }

    @PostMapping(value = "/autorizacoes", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> autorizar(@RequestBody String xmlNfeAssinado) {
        return ResponseEntity.ok(client.autorizarNfe(xmlNfeAssinado));
    }

    @PostMapping(value = "/eventos", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> enviarEvento(@RequestBody String xmlEventoAssinado) {
        return ResponseEntity.ok(client.enviarEvento(xmlEventoAssinado));
    }

    @PostMapping(value = "/inutilizacoes", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> inutilizar(@RequestBody String xmlInutAssinado) {
        return ResponseEntity.ok(client.inutilizarNumeracao(xmlInutAssinado));
    }

    @PostMapping(value = "/distribuicao-dfe", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> distribuir(@RequestBody String xmlConsulta,
            @RequestParam(defaultValue = "false") boolean confirmarConsulta) {
        if (!confirmarConsulta) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(client.consultarDistribuicaoDfe(xmlConsulta));
    }
}
