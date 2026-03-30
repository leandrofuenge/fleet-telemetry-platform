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

import com.telemetria.api.dto.response.MotoristaDTO;
import com.telemetria.domain.entity.Motorista;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.exception.MotoristaDuplicateException;
import com.telemetria.domain.exception.MotoristaNotFoundException;
import com.telemetria.domain.service.MotoristaService;

@RestController
@RequestMapping("/api/v1/motoristas")
public class MotoristaController {

    private static final Logger log = LoggerFactory.getLogger(MotoristaController.class);
    
    @Autowired
    private MotoristaService service;

    @PostMapping
    public ResponseEntity<Motorista> criar(@RequestBody MotoristaDTO dto) {
        log.info("➕ Requisição para criar novo motorista");
        log.debug("📝 Dados recebidos: Nome='{}', CPF='{}', CNH='{}', Categoria='{}'", 
                 dto.getNome(), dto.getCpf(), dto.getCnh(), dto.getCategoriaCnh());
        
        // Validação básica dos dados
        log.debug("🔍 Validando dados do motorista...");
        
        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            log.warn("⚠️ Nome é obrigatório");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome é obrigatório");
        }
        if (dto.getCpf() == null || dto.getCpf().trim().isEmpty()) {
            log.warn("⚠️ CPF é obrigatório");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CPF é obrigatório");
        }
        if (dto.getCnh() == null || dto.getCnh().trim().isEmpty()) {
            log.warn("⚠️ CNH é obrigatória");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CNH é obrigatória");
        }
        if (dto.getCategoriaCnh() == null || dto.getCategoriaCnh().trim().isEmpty()) {
            log.warn("⚠️ Categoria da CNH é obrigatória");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Categoria da CNH é obrigatória");
        }
        
        log.debug("✅ Validação básica OK");

        Motorista motorista = new Motorista();
        motorista.setNome(dto.getNome());
        motorista.setCpf(dto.getCpf());
        motorista.setCnh(dto.getCnh());
        motorista.setCategoriaCnh(dto.getCategoriaCnh());
        
        log.debug("📝 Motorista a ser salvo: {}", motorista);
        
        try {
            log.debug("🔄 Chamando service.salvar()...");
            Motorista saved = service.salvar(motorista);
            log.info("✅ Motorista criado com sucesso - ID: {}, Nome: {}, CPF: {}", 
                    saved.getId(), saved.getNome(), saved.getCpf());
            log.debug("📝 Motorista salvo: {}", saved);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            
        } catch (MotoristaDuplicateException e) {
            log.error("❌ Motorista duplicado: {}", e.getMessage());
            throw new BusinessException(ErrorCode.MOTORISTA_DUPLICATE, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao criar motorista: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping
    public List<Motorista> listar() {
        log.info("📋 Requisição para listar todos os motoristas");
        log.debug("🔍 Chamando service.listar()...");
        
        List<Motorista> motoristas = service.listar();
        
        log.debug("📊 Total de motoristas encontrados: {}", motoristas.size());
        
        if (motoristas.isEmpty()) {
            log.warn("⚠️ Nenhum motorista cadastrado encontrado");
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, "Nenhum motorista cadastrado");
        }
        
        log.info("✅ Retornando {} motoristas", motoristas.size());
        log.trace("📝 Motoristas: {}", motoristas);
        
        return motoristas;
    }

    @GetMapping("/{id}")
    public Motorista buscar(@PathVariable Long id) {
        log.info("🔍 Requisição para buscar motorista por ID: {}", id);
        log.debug("🔍 Chamando service.buscarPorId({})...", id);
        
        try {
            Motorista motorista = service.buscarPorId(id);
            log.info("✅ Motorista encontrado - ID: {}, Nome: {}, CPF: {}", 
                    id, motorista.getNome(), motorista.getCpf());
            log.debug("📝 Motorista completo: {}", motorista);
            
            return motorista;
            
        } catch (MotoristaNotFoundException e) {
            log.error("❌ Motorista não encontrado com ID: {}", id);
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, id.toString());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao buscar motorista {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/cpf/{cpf}")
    public Motorista buscarPorCpf(@PathVariable String cpf) {
        log.info("🔍 Requisição para buscar motorista por CPF: {}", cpf);
        log.debug("🔍 Chamando service.buscarPorCpf({})...", cpf);
        
        try {
            Motorista motorista = service.buscarPorCpf(cpf);
            log.info("✅ Motorista encontrado - ID: {}, Nome: {}", 
                    motorista.getId(), motorista.getNome());
            log.debug("📝 Motorista completo: {}", motorista);
            
            return motorista;
            
        } catch (MotoristaNotFoundException e) {
            log.error("❌ Motorista não encontrado com CPF: {}", cpf);
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, "CPF: " + cpf);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Motorista> atualizar(@PathVariable Long id,
                                @RequestBody MotoristaDTO dto) {
        log.info("🔄 Requisição para atualizar motorista ID: {}", id);
        log.debug("📝 Dados para atualização: Nome='{}', CPF='{}', CNH='{}', Categoria='{}'", 
                 dto.getNome(), dto.getCpf(), dto.getCnh(), dto.getCategoriaCnh());
        
        // Validação básica dos dados
        log.debug("🔍 Validando dados do motorista...");
        
        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            log.warn("⚠️ Nome é obrigatório");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome é obrigatório");
        }
        if (dto.getCpf() == null || dto.getCpf().trim().isEmpty()) {
            log.warn("⚠️ CPF é obrigatório");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CPF é obrigatório");
        }
        if (dto.getCnh() == null || dto.getCnh().trim().isEmpty()) {
            log.warn("⚠️ CNH é obrigatória");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CNH é obrigatória");
        }
        if (dto.getCategoriaCnh() == null || dto.getCategoriaCnh().trim().isEmpty()) {
            log.warn("⚠️ Categoria da CNH é obrigatória");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Categoria da CNH é obrigatória");
        }
        
        log.debug("✅ Validação básica OK");

        try {
            log.debug("🔍 Buscando motorista original ID: {}...", id);
            Motorista motorista = service.buscarPorId(id);
            log.debug("📝 Motorista original: {}", motorista);

            // Log das alterações
            if (!motorista.getNome().equals(dto.getNome())) {
                log.debug("📝 Nome alterado: '{}' → '{}'", motorista.getNome(), dto.getNome());
            }
            if (!motorista.getCpf().equals(dto.getCpf())) {
                log.debug("📝 CPF alterado: '{}' → '{}'", motorista.getCpf(), dto.getCpf());
            }
            if (!motorista.getCnh().equals(dto.getCnh())) {
                log.debug("📝 CNH alterada: '{}' → '{}'", motorista.getCnh(), dto.getCnh());
            }
            if (!motorista.getCategoriaCnh().equals(dto.getCategoriaCnh())) {
                log.debug("📝 Categoria CNH alterada: '{}' → '{}'", 
                         motorista.getCategoriaCnh(), dto.getCategoriaCnh());
            }
            
            motorista.setNome(dto.getNome());
            motorista.setCpf(dto.getCpf());
            motorista.setCnh(dto.getCnh());
            motorista.setCategoriaCnh(dto.getCategoriaCnh());
            
            log.debug("🔄 Chamando service.salvar() para atualização...");
            Motorista updated = service.salvar(motorista);
            
            log.info("✅ Motorista {} atualizado com sucesso", id);
            log.debug("📝 Motorista após atualização: {}", updated);
            
            return ResponseEntity.ok(updated);
            
        } catch (MotoristaNotFoundException e) {
            log.error("❌ Motorista não encontrado com ID: {} para atualização", id);
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, id.toString());
        } catch (MotoristaDuplicateException e) {
            log.error("❌ Motorista duplicado ao atualizar: {}", e.getMessage());
            throw new BusinessException(ErrorCode.MOTORISTA_DUPLICATE, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao atualizar motorista {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        log.info("🗑️ Requisição para deletar motorista ID: {}", id);
        log.debug("🔍 Chamando service.deletar({})...", id);
        
        try {
            service.deletar(id);
            log.info("✅ Motorista {} deletado com sucesso", id);
            
            return ResponseEntity.noContent().build();
            
        } catch (MotoristaNotFoundException e) {
            log.error("❌ Motorista não encontrado com ID: {} para deleção", id);
            throw new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, id.toString());
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao deletar motorista {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}