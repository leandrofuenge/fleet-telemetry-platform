package com.telemetria.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.Cte;
import com.telemetria.domain.entity.Mdfe;
import com.telemetria.domain.service.FiscalService;
import com.telemetria.infrastructure.persistence.MdfeRepository;

@RestController
@RequestMapping("/api/v1/fiscal")
public class FiscalController {
    private final FiscalService fiscalService;
    private final MdfeRepository mdfeRepository;

    public FiscalController(FiscalService fiscalService, MdfeRepository mdfeRepository) {
        this.fiscalService = fiscalService;
        this.mdfeRepository = mdfeRepository;
    }

    @PostMapping("/mdfe")
    public ResponseEntity<Mdfe> criarMdfe(@RequestBody Mdfe mdfe) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscalService.salvarMdfe(mdfe));
    }

    @PostMapping("/cte")
    public ResponseEntity<Cte> criarCte(@RequestBody Cte cte) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscalService.salvarCte(cte));
    }

    @GetMapping("/mdfe/{chave}")
    public Mdfe consultarMdfe(@PathVariable String chave) {
        return mdfeRepository.findByChaveMdfe(chave).orElseThrow();
    }
}
