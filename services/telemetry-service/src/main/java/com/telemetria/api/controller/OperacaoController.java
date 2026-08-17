package com.telemetria.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.MensagemViagem;
import com.telemetria.domain.entity.Multa;
import com.telemetria.domain.entity.OcorrenciaOperacional;
import com.telemetria.domain.service.OperacaoService;

@RestController
@RequestMapping("/api/v1")
public class OperacaoController {
    private final OperacaoService operacaoService;

    public OperacaoController(OperacaoService operacaoService) {
        this.operacaoService = operacaoService;
    }

    @PostMapping("/mensagens")
    public ResponseEntity<MensagemViagem> enviarMensagem(@RequestBody MensagemViagem mensagem,
            @RequestParam(defaultValue = "false") boolean veiculoParado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operacaoService.enviar(mensagem, veiculoParado));
    }

    @GetMapping("/viagens/{id}/mensagens")
    public List<MensagemViagem> historico(@PathVariable Long id) {
        return operacaoService.historico(id);
    }

    @PostMapping("/ocorrencias")
    public OcorrenciaOperacional registrarOcorrencia(@RequestBody OcorrenciaOperacional ocorrencia) {
        return operacaoService.registrar(ocorrencia);
    }

    @PostMapping("/multas")
    public Multa registrarMulta(@RequestBody Multa multa) {
        return operacaoService.registrarMulta(multa);
    }
}
