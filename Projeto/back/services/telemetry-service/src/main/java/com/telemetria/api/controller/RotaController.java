package com.telemetria.api.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.exception.RotaDuplicateException;
import com.telemetria.domain.exception.RotaNotFoundException;
import com.telemetria.domain.exception.RotaValidationException;
import com.telemetria.domain.service.RotaService;

@RestController
@RequestMapping("/api/v1/rotas")
public class RotaController {
    
    private static final Logger log = LoggerFactory.getLogger(RotaController.class);
    private final RotaService service;
    
    public RotaController(RotaService service) {
        this.service = service;
        log.info("✅ RotaController inicializado");
    }
    
    @PostMapping
    public ResponseEntity<Rota> criar(@RequestBody Rota rota) {
        log.info("➕ Requisição para criar nova rota");
        log.debug("📝 Dados recebidos: {}", rota);
        
        // Validação básica dos dados
        log.debug("🔍 Validando dados da rota...");
        
        if (rota.getNome() == null || rota.getNome().trim().isEmpty()) {
            log.warn("⚠️ Nome da rota é obrigatório");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome da rota é obrigatório");
        }
        if (rota.getOrigem() == null || rota.getDestino() == null) {
            log.warn("⚠️ Origem e destino são obrigatórios - Origem: {}, Destino: {}", 
                    rota.getOrigem(), rota.getDestino());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Origem e destino são obrigatórios");
        }
        
        log.debug("✅ Validação básica OK");
        
        try {
            log.debug("🔄 Chamando service.salvar()...");
            Rota saved = service.salvar(rota);
            log.info("✅ Rota criada com sucesso - ID: {}", saved.getId());
            log.debug("📝 Rota salva: {}", saved);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            
        } catch (RotaDuplicateException e) {
            log.error("❌ Rota duplicada: {}", e.getMessage());
            throw new BusinessException(ErrorCode.ROTA_DUPLICATE, e.getMessage());
        } catch (RotaValidationException e) {
            log.error("❌ Validação da rota falhou: {}", e.getMessage());
            throw new BusinessException(ErrorCode.ROTA_INVALID, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao criar rota: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Rota>> listar() {
        log.info("📋 Requisição para listar todas as rotas");
        log.debug("🔍 Chamando service.listar()...");
        
        List<Rota> rotas = service.listar();
        
        log.debug("📊 Total de rotas encontradas: {}", rotas.size());
        
        if (rotas.isEmpty()) {
            log.warn("⚠️ Nenhuma rota cadastrada encontrada");
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, "Nenhuma rota cadastrada");
        }
        
        log.info("✅ Retornando {} rotas", rotas.size());
        log.trace("📝 Rotas: {}", rotas);
        
        return ResponseEntity.ok(rotas);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Rota> buscar(@PathVariable Long id) {
        log.info("🔍 Requisição para buscar rota por ID: {}", id);
        log.debug("🔍 Chamando service.buscarPorId({})...", id);
        
        try {
            Rota rota = service.buscarPorId(id);
            log.info("✅ Rota encontrada - ID: {}, Origem: {}, Destino: {}", 
                    id, rota.getOrigem(), rota.getDestino());
            log.debug("📝 Rota completa: {}", rota);
            
            return ResponseEntity.ok(rota);
            
        } catch (RotaNotFoundException e) {
            log.error("❌ Rota não encontrada com ID: {}", id);
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, id.toString());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao buscar rota {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    @PutMapping("/{id}")  
    public ResponseEntity<Rota> atualizar(@PathVariable Long id, @RequestBody Rota rota) {
        log.info("🔄 Requisição para atualizar rota ID: {}", id);
        log.debug("📝 Dados para atualização: {}", rota);
        
        // Validação básica dos dados
        log.debug("🔍 Validando dados da rota...");
        
        if (rota.getNome() != null && rota.getNome().trim().isEmpty()) {
            log.warn("⚠️ Nome da rota não pode ser vazio");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome da rota não pode ser vazio");
        }
        
        log.debug("✅ Validação básica OK");
        
        try {
            log.debug("🔄 Chamando service.atualizar({}, ...)", id);
            Rota updated = service.atualizar(id, rota);
            log.info("✅ Rota {} atualizada com sucesso", id);
            log.debug("📝 Rota após atualização: {}", updated);
            
            return ResponseEntity.ok(updated);
            
        } catch (RotaNotFoundException e) {
            log.error("❌ Rota não encontrada com ID: {} para atualização", id);
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, id.toString());
        } catch (RotaDuplicateException e) {
            log.error("❌ Rota duplicada ao atualizar: {}", e.getMessage());
            throw new BusinessException(ErrorCode.ROTA_DUPLICATE, e.getMessage());
        } catch (RotaValidationException e) {
            log.error("❌ Validação falhou ao atualizar rota: {}", e.getMessage());
            throw new BusinessException(ErrorCode.ROTA_INVALID, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao atualizar rota {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        log.info("🗑️ Requisição para deletar rota ID: {}", id);
        log.debug("🔍 Chamando service.deletar({})...", id);
        
        try {
            service.deletar(id);
            log.info("✅ Rota {} deletada com sucesso", id);
            
            return ResponseEntity.noContent().build();
            
        } catch (RotaNotFoundException e) {
            log.error("❌ Rota não encontrada com ID: {} para deleção", id);
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, id.toString());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao deletar rota {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}