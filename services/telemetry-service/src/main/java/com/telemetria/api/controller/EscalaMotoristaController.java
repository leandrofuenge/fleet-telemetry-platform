package com.telemetria.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.EscalaMotorista;
import com.telemetria.domain.service.EscalaMotoristaService;
@RestController
@RequestMapping("/api/v1/escalas")
public class EscalaMotoristaController {
    private final EscalaMotoristaService escalaService;

    public EscalaMotoristaController(EscalaMotoristaService escalaService) {
        this.escalaService = escalaService;
    }

    @GetMapping
    public List<EscalaMotorista> listar() {
        return escalaService.listar();
    }

    @PostMapping
    public ResponseEntity<EscalaMotorista> criar(@RequestBody EscalaMotorista escala) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escalaService.criar(escala));
    }

    @PostMapping("/{id}/confirmar")
    public EscalaMotorista confirmar(@PathVariable Long id) {
        return escalaService.confirmar(id);
    }
}
