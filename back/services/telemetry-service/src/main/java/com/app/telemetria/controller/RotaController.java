package com.app.telemetria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.app.telemetria.entity.Rota;
import com.app.telemetria.service.RotaService;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.exception.BusinessException;
import com.app.telemetria.exception.RotaNotFoundException;
import com.app.telemetria.exception.RotaDuplicateException;
import com.app.telemetria.exception.RotaValidationException;

@RestController
@RequestMapping("/api/v1/rotas")
public class RotaController {
    
    private final RotaService service;
    
    public RotaController(RotaService service) {
        this.service = service;
    }
    
    @PostMapping
    public ResponseEntity<Rota> criar(@RequestBody Rota rota) {
        // Validação básica dos dados
        if (rota.getNome() == null || rota.getNome().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome da rota é obrigatório");
        }
        if (rota.getOrigem() == null || rota.getDestino() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Origem e destino são obrigatórios");
        }
        
        try {
            Rota saved = service.salvar(rota);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RotaDuplicateException e) {
            throw new BusinessException(ErrorCode.ROTA_DUPLICATE, e.getMessage());
        } catch (RotaValidationException e) {
            throw new BusinessException(ErrorCode.ROTA_INVALID, e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Rota>> listar() {
        List<Rota> rotas = service.listar();
        if (rotas.isEmpty()) {
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, "Nenhuma rota cadastrada");
        }
        return ResponseEntity.ok(rotas);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Rota> buscar(@PathVariable Long id) {
        try {
            Rota rota = service.buscarPorId(id);
            return ResponseEntity.ok(rota);
        } catch (RotaNotFoundException e) {
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, id.toString());
        }
    }
    
    @PutMapping("/{id}")  
    public ResponseEntity<Rota> atualizar(@PathVariable Long id, @RequestBody Rota rota) {
        // Validação básica dos dados
        if (rota.getNome() != null && rota.getNome().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome da rota não pode ser vazio");
        }
        
        try {
            Rota updated = service.atualizar(id, rota);
            return ResponseEntity.ok(updated);
        } catch (RotaNotFoundException e) {
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, id.toString());
        } catch (RotaDuplicateException e) {
            throw new BusinessException(ErrorCode.ROTA_DUPLICATE, e.getMessage());
        } catch (RotaValidationException e) {
            throw new BusinessException(ErrorCode.ROTA_INVALID, e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        try {
            service.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (RotaNotFoundException e) {
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, id.toString());
        }
    }
}