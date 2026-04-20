package com.telemetria.api.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.telemetria.api.dto.response.VeiculoDTO;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.exception.VeiculoDuplicateException;
import com.telemetria.domain.exception.VeiculoNotFoundException;
import com.telemetria.domain.service.VeiculoService;

@RestController
@RequestMapping("/api/v1/veiculos")
public class VeiculoController {

    private static final Logger log = LoggerFactory.getLogger(VeiculoController.class);
    
    @Autowired
    private VeiculoService service;

    @PostMapping
    public ResponseEntity<VeiculoDTO> criar(@RequestBody Veiculo veiculo) {
        log.info("➕ Requisição para criar novo veículo");
        log.debug("📝 Dados recebidos: Placa='{}', Modelo='{}', Marca='{}', Capacidade={}kg, Ano={}", 
                 veiculo.getPlaca(), veiculo.getModelo(), veiculo.getMarca(), 
                 veiculo.getCapacidadeCarga(), veiculo.getAnoFabricacao());
        
        // Validação básica dos dados
        log.debug("🔍 Validando dados do veículo...");
        
        if (veiculo.getPlaca() == null || veiculo.getPlaca().trim().isEmpty()) {
            log.error("❌ Placa é obrigatória");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Placa é obrigatória");
        }
        if (veiculo.getModelo() == null || veiculo.getModelo().trim().isEmpty()) {
            log.error("❌ Modelo é obrigatório");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Modelo é obrigatório");
        }
        if (veiculo.getCapacidadeCarga() == null || veiculo.getCapacidadeCarga() <= 0) {
            log.error("❌ Capacidade de carga deve ser maior que zero: {}", veiculo.getCapacidadeCarga());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Capacidade de carga deve ser maior que zero");
        }
        
        log.debug("✅ Validação básica OK");
        
        try {
            log.debug("🔄 Chamando service.salvar()...");
            VeiculoDTO saved = service.salvar(veiculo);
            log.info("✅ Veículo criado com sucesso - ID: {}, Placa: {}", saved.getId(), saved.getPlaca());
            log.debug("📝 Veículo salvo: {}", saved);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            
        } catch (VeiculoDuplicateException e) {
            log.error("❌ Veículo duplicado: {}", e.getMessage());
            throw new BusinessException(ErrorCode.VEICULO_DUPLICATE, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao criar veículo: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<VeiculoDTO>> listar() {
        log.info("📋 Requisição para listar todos os veículos");
        log.debug("🔍 Chamando service.listarTodos()...");
        
        List<VeiculoDTO> veiculos = service.listarTodos();
        
        log.debug("📊 Total de veículos encontrados: {}", veiculos.size());
        
        if (veiculos.isEmpty()) {
            log.warn("⚠️ Nenhum veículo cadastrado encontrado");
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, "Nenhum veículo cadastrado");
        }
        
        log.info("✅ Retornando {} veículos", veiculos.size());
        log.trace("📝 IDs dos veículos: {}", veiculos.stream().map(VeiculoDTO::getId).toList());
        
        return ResponseEntity.ok(veiculos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoDTO> buscar(@PathVariable Long id) {
        log.info("🔍 Requisição para buscar veículo por ID: {}", id);
        log.debug("🔍 Chamando service.buscarPorId({})...", id);
        
        try {
            VeiculoDTO veiculo = service.buscarPorId(id);
            log.info("✅ Veículo encontrado - ID: {}, Placa: {}, Modelo: {}", 
                    id, veiculo.getPlaca(), veiculo.getModelo());
            log.debug("📝 Veículo completo: {}", veiculo);
            
            return ResponseEntity.ok(veiculo);
            
        } catch (VeiculoNotFoundException e) {
            log.error("❌ Veículo não encontrado com ID: {}", id);
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, id.toString());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao buscar veículo {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/placa/{placa}")
    public ResponseEntity<VeiculoDTO> buscarPorPlaca(@PathVariable String placa) {
        log.info("🔍 Requisição para buscar veículo por placa: {}", placa);
        log.debug("🔍 Chamando service.buscarPorPlaca({})...", placa);
        
        try {
            VeiculoDTO veiculo = service.buscarPorPlaca(placa);
            log.info("✅ Veículo encontrado - ID: {}, Modelo: {}", veiculo.getId(), veiculo.getModelo());
            log.debug("📝 Veículo completo: {}", veiculo);
            
            return ResponseEntity.ok(veiculo);
            
        } catch (VeiculoNotFoundException e) {
            log.error("❌ Veículo não encontrado com placa: {}", placa);
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, "Placa: " + placa);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoDTO> atualizar(@PathVariable Long id,
                                @RequestBody Veiculo veiculo) {
        log.info("🔄 Requisição para atualizar veículo ID: {}", id);
        log.debug("📝 Dados para atualização: Placa='{}', Modelo='{}', Marca='{}', Capacidade={}kg", 
                 veiculo.getPlaca(), veiculo.getModelo(), veiculo.getMarca(), 
                 veiculo.getCapacidadeCarga());
        
        // Validação básica dos dados
        log.debug("🔍 Validando dados do veículo...");
        
        if (veiculo.getPlaca() != null && veiculo.getPlaca().trim().isEmpty()) {
            log.warn("⚠️ Placa não pode ser vazia");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Placa não pode ser vazia");
        }
        if (veiculo.getCapacidadeCarga() != null && veiculo.getCapacidadeCarga() <= 0) {
            log.warn("⚠️ Capacidade de carga deve ser maior que zero: {}", veiculo.getCapacidadeCarga());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Capacidade de carga deve ser maior que zero");
        }
        
        log.debug("✅ Validação básica OK");
        
        try {
            log.debug("🔍 Buscando veículo original ID: {}...", id);
            VeiculoDTO veiculoExistente = service.buscarPorId(id);
            log.debug("📝 Veículo original: {}", veiculoExistente);
            
            // Log das alterações
            if (veiculo.getPlaca() != null && !veiculo.getPlaca().equals(veiculoExistente.getPlaca())) {
                log.debug("📝 Placa alterada: '{}' → '{}'", veiculoExistente.getPlaca(), veiculo.getPlaca());
            }
            if (veiculo.getModelo() != null && !veiculo.getModelo().equals(veiculoExistente.getModelo())) {
                log.debug("📝 Modelo alterado: '{}' → '{}'", veiculoExistente.getModelo(), veiculo.getModelo());
            }
            
            log.debug("🔄 Chamando service.atualizar({}, ...)", id);
            VeiculoDTO updated = service.atualizar(id, veiculo);
            
            log.info("✅ Veículo {} atualizado com sucesso", id);
            log.debug("📝 Veículo após atualização: {}", updated);
            
            return ResponseEntity.ok(updated);
            
        } catch (VeiculoNotFoundException e) {
            log.error("❌ Veículo não encontrado com ID: {} para atualização", id);
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, id.toString());
        } catch (VeiculoDuplicateException e) {
            log.error("❌ Veículo duplicado ao atualizar: {}", e.getMessage());
            throw new BusinessException(ErrorCode.VEICULO_DUPLICATE, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao atualizar veículo {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        log.info("🗑️ Requisição para deletar veículo ID: {}", id);
        log.debug("🔍 Chamando service.deletar({})...", id);
        
        try {
            service.deletar(id);
            log.info("✅ Veículo {} deletado com sucesso", id);
            
            return ResponseEntity.noContent().build();
            
        } catch (VeiculoNotFoundException e) {
            log.error("❌ Veículo não encontrado com ID: {} para deleção", id);
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, id.toString());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao deletar veículo {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    // ================ MÉTODOS ADICIONAIS PARA DEBUG ================

    @GetMapping("/debug/status")
    public ResponseEntity<String> status() {
        log.info("🔍 Verificando status do serviço de veículos");
        return ResponseEntity.ok("Veículo service está operacional");
    }

    @GetMapping("/debug/contagem")
    public ResponseEntity<Long> contarVeiculos() {
        log.info("🔍 Contando total de veículos");
        List<VeiculoDTO> veiculos = service.listarTodos();
        log.info("✅ Total de veículos: {}", veiculos.size());
        return ResponseEntity.ok((long) veiculos.size());
    }
}