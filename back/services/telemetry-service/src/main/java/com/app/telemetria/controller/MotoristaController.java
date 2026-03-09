package com.app.telemetria.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.app.telemetria.dto.MotoristaDTO;
import com.app.telemetria.entity.Motorista;
import com.app.telemetria.service.MotoristaService;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.exception.BusinessException;
import com.app.telemetria.exception.MotoristaNotFoundException;
import com.app.telemetria.exception.MotoristaDuplicateException;

@RestController
@RequestMapping("/api/v1/motoristas")
public class MotoristaController {

    @Autowired
    private MotoristaService service;

    @PostMapping
    public ResponseEntity<Motorista> criar(@RequestBody MotoristaDTO dto) {
        // Validação básica dos dados
        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome é obrigatório");
        }
        if (dto.getCpf() == null || dto.getCpf().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CPF é obrigatório");
        }
        if (dto.getCnh() == null || dto.getCnh().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CNH é obrigatória");
        }
        if (dto.getCategoriaCnh() == null || dto.getCategoriaCnh().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Categoria da CNH é obrigatória");
        }

        Motorista motorista = new Motorista();
        motorista.setNome(dto.getNome());
        motorista.setCpf(dto.getCpf());
        motorista.setCnh(dto.getCnh());
        motorista.setCategoriaCnh(dto.getCategoriaCnh());
        
        try {
            Motorista saved = service.salvar(motorista);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (MotoristaDuplicateException e) {
            throw new BusinessException(ErrorCode.MOTORISTA_DUPLICATE, e.getMessage());
        }
    }

    @GetMapping
    public List<Motorista> listar() {
        List<Motorista> motoristas = service.listar();
        if (motoristas.isEmpty()) {
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, "Nenhum motorista cadastrado");
        }
        return motoristas;
    }

    @GetMapping("/{id}")
    public Motorista buscar(@PathVariable Long id) {
        try {
            return service.buscarPorId(id);
        } catch (MotoristaNotFoundException e) {
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, id.toString());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Motorista> atualizar(@PathVariable Long id,
                                @RequestBody MotoristaDTO dto) {
        // Validação básica dos dados
        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome é obrigatório");
        }
        if (dto.getCpf() == null || dto.getCpf().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CPF é obrigatório");
        }
        if (dto.getCnh() == null || dto.getCnh().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CNH é obrigatória");
        }
        if (dto.getCategoriaCnh() == null || dto.getCategoriaCnh().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Categoria da CNH é obrigatória");
        }

        try {
            Motorista motorista = service.buscarPorId(id);
            
            motorista.setNome(dto.getNome());
            motorista.setCpf(dto.getCpf());
            motorista.setCnh(dto.getCnh());
            motorista.setCategoriaCnh(dto.getCategoriaCnh());
            
            Motorista updated = service.salvar(motorista);
            return ResponseEntity.ok(updated);
            
        } catch (MotoristaNotFoundException e) {
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, id.toString());
        } catch (MotoristaDuplicateException e) {
            throw new BusinessException(ErrorCode.MOTORISTA_DUPLICATE, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        try {
            service.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (MotoristaNotFoundException e) {
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, id.toString());
        }
    }
}