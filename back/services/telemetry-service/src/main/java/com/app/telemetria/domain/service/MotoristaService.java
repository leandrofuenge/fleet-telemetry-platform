package com.app.telemetria.domain.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.app.telemetria.domain.entity.Motorista;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.infrastructure.persistence.MotoristaRepository;

@Service
public class MotoristaService {

    private static final Logger log = LoggerFactory.getLogger(MotoristaService.class);

    @Autowired
    private MotoristaRepository repository;

    public Motorista salvar(Motorista motorista) {
        boolean isNovo = motorista.getId() == null;
        
        if (isNovo) {
            log.info("➕ Salvando novo motorista - Nome: {}, CPF: {}, CNH: {}", 
                    motorista.getNome(), motorista.getCpf(), motorista.getCnh());
        } else {
            log.info("🔄 Atualizando motorista ID: {} - Nome: {}, CPF: {}, CNH: {}", 
                    motorista.getId(), motorista.getNome(), motorista.getCpf(), motorista.getCnh());
        }
        
        log.debug("📝 Dados do motorista: {}", motorista);
        
        try {
            Motorista salvo = repository.save(motorista);
            
            if (isNovo) {
                log.info("✅ Motorista salvo com ID: {}", salvo.getId());
            } else {
                log.info("✅ Motorista ID: {} atualizado com sucesso", salvo.getId());
            }
            log.debug("📝 Motorista salvo completo: {}", salvo);
            
            return salvo;
            
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause().getMessage();
            log.error("❌ Violação de integridade ao salvar motorista: {}", message);
            
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                log.debug("🔍 Mensagem de erro: {}", lowerMessage);

                if (lowerMessage.contains("cpf")) {
                    log.error("❌ CPF duplicado: {}", motorista.getCpf());
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com o CPF: " + motorista.getCpf()
                    );
                }

                if (lowerMessage.contains("cnh")) {
                    log.error("❌ CNH duplicada: {}", motorista.getCnh());
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com a CNH: " + motorista.getCnh()
                    );
                }
                
                if (lowerMessage.contains("email")) {
                    log.error("❌ Email duplicado");
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com este email"
                    );
                }
            }

            log.error("❌ Erro interno de integridade: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao salvar motorista: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Motorista> listar() {
        log.info("📋 Listando todos os motoristas");
        log.debug("🔍 Consultando repositório...");
        
        List<Motorista> motoristas = repository.findAll();
        
        log.info("✅ Total de motoristas encontrados: {}", motoristas.size());
        log.debug("📊 IDs dos motoristas: {}", motoristas.stream().map(Motorista::getId).toList());
        
        return motoristas;
    }

    public Motorista buscarPorId(Long id) {
        log.info("🔍 Buscando motorista por ID: {}", id);
        log.debug("🔍 Consultando repositório para ID: {}", id);
        
        Motorista motorista = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Motorista não encontrado com id: {}", id);
                    return new BusinessException(
                            ErrorCode.MOTORISTA_NOT_FOUND,
                            "Motorista não encontrado com id: " + id
                    );
                });

        log.info("✅ Motorista encontrado - Nome: {}, CPF: {}, CNH: {}", 
                motorista.getNome(), motorista.getCpf(), motorista.getCnh());
        log.debug("📝 Motorista completo: {}", motorista);
        
        return motorista;
    }

    public Motorista buscarPorCpf(String cpf) {
        log.info("🔍 Buscando motorista por CPF: {}", cpf);
        log.debug("🔍 Consultando repositório para CPF: {}", cpf);
        
        Motorista motorista = repository.findByCpf(cpf)
                .orElseThrow(() -> {
                    log.error("❌ Motorista não encontrado com CPF: {}", cpf);
                    return new BusinessException(
                            ErrorCode.MOTORISTA_NOT_FOUND,
                            "Motorista não encontrado com CPF: " + cpf
                    );
                });

        log.info("✅ Motorista encontrado - ID: {}, Nome: {}", motorista.getId(), motorista.getNome());
        log.debug("📝 Motorista completo: {}", motorista);
        
        return motorista;
    }

    public Motorista atualizar(Long id, Motorista dados) {
        log.info("🔄 Atualizando motorista ID: {}", id);
        log.debug("📝 Dados para atualização: Nome='{}', CPF='{}', CNH='{}', Categoria='{}'", 
                 dados.getNome(), dados.getCpf(), dados.getCnh(), dados.getCategoriaCnh());

        Motorista motorista = buscarPorId(id);
        log.debug("📝 Motorista original: {}", motorista);

        // Log das alterações
        if (!motorista.getNome().equals(dados.getNome())) {
            log.debug("📝 Nome alterado: '{}' → '{}'", motorista.getNome(), dados.getNome());
        }
        if (!motorista.getCpf().equals(dados.getCpf())) {
            log.debug("📝 CPF alterado: '{}' → '{}'", motorista.getCpf(), dados.getCpf());
        }
        if (!motorista.getCnh().equals(dados.getCnh())) {
            log.debug("📝 CNH alterada: '{}' → '{}'", motorista.getCnh(), dados.getCnh());
        }
        if (!motorista.getCategoriaCnh().equals(dados.getCategoriaCnh())) {
            log.debug("📝 Categoria CNH alterada: '{}' → '{}'", 
                     motorista.getCategoriaCnh(), dados.getCategoriaCnh());
        }

        motorista.setNome(dados.getNome());
        motorista.setCpf(dados.getCpf());
        motorista.setCnh(dados.getCnh());
        motorista.setCategoriaCnh(dados.getCategoriaCnh());

        try {
            log.debug("🔄 Salvando motorista atualizado...");
            Motorista atualizado = repository.save(motorista);
            log.info("✅ Motorista ID: {} atualizado com sucesso", id);
            log.debug("📝 Motorista após atualização: {}", atualizado);
            
            return atualizado;
            
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause().getMessage();
            log.error("❌ Violação de integridade ao atualizar motorista {}: {}", id, message);
            
            if (message != null) {
                String lowerMessage = message.toLowerCase();

                if (lowerMessage.contains("cpf")) {
                    log.error("❌ CPF duplicado: {}", dados.getCpf());
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com o CPF: " + dados.getCpf()
                    );
                }

                if (lowerMessage.contains("cnh")) {
                    log.error("❌ CNH duplicada: {}", dados.getCnh());
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com a CNH: " + dados.getCnh()
                    );
                }
            }

            log.error("❌ Erro interno de integridade: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao atualizar motorista {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public void deletar(Long id) {
        log.info("🗑️ Deletando motorista ID: {}", id);
        
        Motorista motorista = buscarPorId(id);
        log.debug("📝 Motorista a ser deletado: {}", motorista);
        
        repository.delete(motorista);
        log.info("✅ Motorista ID: {} deletado com sucesso", id);
    }
}