package com.telemetria.integration.sefaz.nfe;

import java.util.function.Function;

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
    private final NfeBase64Codec base64Codec;

    public NfeController(NfeClient client, NfeBase64Codec base64Codec) {
        this.client = client;
        this.base64Codec = base64Codec;
    }

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

    @PostMapping(value = "/autorizacoes/base64", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NfeBase64Response> autorizarBase64(@RequestBody NfeBase64Request request) {
        return processarBase64(request, client::autorizarNfe);
    }

    @PostMapping(value = "/eventos", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> enviarEvento(@RequestBody String xmlEventoAssinado) {
        return ResponseEntity.ok(client.enviarEvento(xmlEventoAssinado));
    }

    @PostMapping(value = "/eventos/base64", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NfeBase64Response> enviarEventoBase64(@RequestBody NfeBase64Request request) {
        return processarBase64(request, client::enviarEvento);
    }

    @PostMapping(value = "/inutilizacoes", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> inutilizar(@RequestBody String xmlInutAssinado) {
        return ResponseEntity.ok(client.inutilizarNumeracao(xmlInutAssinado));
    }

    @PostMapping(value = "/inutilizacoes/base64", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NfeBase64Response> inutilizarBase64(@RequestBody NfeBase64Request request) {
        return processarBase64(request, client::inutilizarNumeracao);
    }

    @PostMapping(value = "/distribuicao-dfe", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> distribuir(@RequestBody String xmlConsulta,
            @RequestParam(defaultValue = "false") boolean confirmarConsulta) {
        if (!confirmarConsulta) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(client.consultarDistribuicaoDfe(xmlConsulta));
    }

    @PostMapping(value = "/distribuicao-dfe/base64", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NfeBase64Response> distribuirBase64(@RequestBody NfeBase64Request request,
            @RequestParam(defaultValue = "false") boolean confirmarConsulta) {
        if (!confirmarConsulta) {
            return ResponseEntity.badRequest().build();
        }
        return processarBase64(request, client::consultarDistribuicaoDfe);
    }

    private ResponseEntity<NfeBase64Response> processarBase64(
            NfeBase64Request request, Function<String, String> operacao) {
        String xmlResposta = operacao.apply(base64Codec.decodificarXml(request));
        return ResponseEntity.ok(base64Codec.codificarResposta(xmlResposta));
    }

}
