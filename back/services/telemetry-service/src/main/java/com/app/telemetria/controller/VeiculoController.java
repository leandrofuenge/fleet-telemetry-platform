package com.app.telemetria.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.app.telemetria.dto.VeiculoDTO;
import com.app.telemetria.entity.Veiculo;
import com.app.telemetria.service.VeiculoService;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.exception.BusinessException;
import com.app.telemetria.exception.VeiculoNotFoundException;
import com.app.telemetria.exception.VeiculoDuplicateException;

@RestController
@RequestMapping("/api/v1/veiculos")
public class VeiculoController {

    @Autowired
    private VeiculoService service;

    @PostMapping
    public ResponseEntity<VeiculoDTO> criar(@RequestBody Veiculo veiculo) {
        // Validação básica dos dados
        if (veiculo.getPlaca() == null || veiculo.getPlaca().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Placa é obrigatória");
        }
        if (veiculo.getModelo() == null || veiculo.getModelo().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Modelo é obrigatório");
        }
        if (veiculo.getCapacidadeCarga() == null || veiculo.getCapacidadeCarga() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Capacidade de carga deve ser maior que zero");
        }
        
        try {
            VeiculoDTO saved = service.salvar(veiculo);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (VeiculoDuplicateException e) {
            throw new BusinessException(ErrorCode.VEICULO_DUPLICATE, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VeiculoDTO>> listar() {
        List<VeiculoDTO> veiculos = service.listarTodos();
        if (veiculos.isEmpty()) {
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, "Nenhum veículo cadastrado");
        }
        return ResponseEntity.ok(veiculos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoDTO> buscar(@PathVariable Long id) {
        try {
            VeiculoDTO veiculo = service.buscarPorId(id);
            return ResponseEntity.ok(veiculo);
        } catch (VeiculoNotFoundException e) {
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, id.toString());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoDTO> atualizar(@PathVariable Long id,
                                @RequestBody Veiculo veiculo) {
        // Validação básica dos dados
        if (veiculo.getPlaca() != null && veiculo.getPlaca().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Placa não pode ser vazia");
        }
        if (veiculo.getCapacidadeCarga() != null && veiculo.getCapacidadeCarga() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Capacidade de carga deve ser maior que zero");
        }
        
        try {
            VeiculoDTO updated = service.atualizar(id, veiculo);
            return ResponseEntity.ok(updated);
        } catch (VeiculoNotFoundException e) {
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, id.toString());
        } catch (VeiculoDuplicateException e) {
            throw new BusinessException(ErrorCode.VEICULO_DUPLICATE, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        try {
            service.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (VeiculoNotFoundException e) {
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, id.toString());
        }
    }
}