// src/main/java/com/telemetria/api/controller/ETAController.java
package com.telemetria.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.api.dto.response.ETAResponseDTO;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.enums.StatusViagem;
import com.telemetria.domain.service.ETACalculationService;
import com.telemetria.infrastructure.persistence.ViagemRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/eta")
@Tag(name = "ETA Dinâmico", description = "Endpoints para consulta de ETA em tempo real")
public class ETAController {
    
    @Autowired
    private ETACalculationService etaCalculationService;
    
    @Autowired
    private ViagemRepository viagemRepository;
    
    @GetMapping("/viagem/{viagemId}")
    @Operation(summary = "Consulta ETA de uma viagem específica")
    public ResponseEntity<ETAResponseDTO> consultarETA(@PathVariable Long viagemId) {
        Viagem viagem = viagemRepository.findById(viagemId).orElse(null);
        if (viagem == null) {
            return ResponseEntity.notFound().build();
        }
        
        ETAResponseDTO response = etaCalculationService.gerarResponseDTO(viagem);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/viagens/ativas")
    @Operation(summary = "Lista ETA de todas as viagens ativas")
    public ResponseEntity<List<ETAResponseDTO>> listarETAAtivos() {
        List<Viagem> viagensAtivas = viagemRepository.findByStatus(StatusViagem.EM_ANDAMENTO.name());
        
        List<ETAResponseDTO> response = viagensAtivas.stream()
            .map(etaCalculationService::gerarResponseDTO)
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/recalcular/{viagemId}")
    @Operation(summary = "Força recálculo manual de ETA para uma viagem")
    public ResponseEntity<ETAResponseDTO> forcarRecalculo(@PathVariable Long viagemId) {
        var resultado = etaCalculationService.recalcularETA(viagemId, "RECALCULO_MANUAL");
        
        if (resultado.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Viagem viagem = viagemRepository.findById(viagemId).orElse(null);
        if (viagem == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(etaCalculationService.gerarResponseDTO(viagem));
    }
}