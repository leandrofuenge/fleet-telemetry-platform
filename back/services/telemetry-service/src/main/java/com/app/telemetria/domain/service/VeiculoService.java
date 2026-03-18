package com.app.telemetria.domain.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.app.telemetria.api.dto.response.VeiculoDTO;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.exception.VeiculoDuplicateException;
import com.app.telemetria.domain.exception.VeiculoNotFoundException;
import com.app.telemetria.infrastructure.persistence.VeiculoRepository;

@Service
public class VeiculoService {

    private static final Logger log = LoggerFactory.getLogger(VeiculoService.class);

    @Autowired
    private VeiculoRepository repository;

    // ================ MÉTODOS CRUD ================

    public VeiculoDTO salvar(Veiculo veiculo) {
        log.info("➕ Salvando novo veículo - Placa: {}, Modelo: {}, Capacidade: {}kg", 
                veiculo.getPlaca(), veiculo.getModelo(), veiculo.getCapacidadeCarga());
        
        log.debug("📝 Dados do veículo: {}", veiculo);
        
        // Validação de placa duplicada
        if (repository.existsByPlaca(veiculo.getPlaca())) {
            log.error("❌ Placa já cadastrada: {}", veiculo.getPlaca());
            throw new VeiculoDuplicateException("Placa '" + veiculo.getPlaca() + "' já está cadastrada");
        }
        
        try {
            Veiculo salvo = repository.save(veiculo);
            log.info("✅ Veículo salvo com ID: {}", salvo.getId());
            log.debug("📝 Veículo salvo completo: {}", salvo);
            
            return toDTO(salvo);
            
        } catch (DataIntegrityViolationException e) {
            log.error("❌ Erro de integridade ao salvar veículo: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Erro ao salvar veículo");
        }
    }

    public List<VeiculoDTO> listarTodos() {
        log.info("📋 Listando todos os veículos");
        log.debug("🔍 Consultando repositório...");
        
        List<Veiculo> veiculos = repository.findAll();
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Total de veículos encontrados: {}", veiculos.size());
        log.debug("📊 IDs dos veículos: {}", veiculos.stream().map(Veiculo::getId).toList());
        
        return dtos;
    }

    public VeiculoDTO buscarPorId(Long id) {
        log.info("🔍 Buscando veículo por ID: {}", id);
        log.debug("🔍 Consultando repositório para ID {}", id);
        
        Veiculo veiculo = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com ID: {}", id);
                    return new VeiculoNotFoundException("Veículo não encontrado com ID: " + id);
                });

        log.info("✅ Veículo encontrado - Placa: {}, Modelo: {}", veiculo.getPlaca(), veiculo.getModelo());
        log.debug("📝 Veículo completo: {}", veiculo);
        
        return toDTO(veiculo);
    }

    /**
     * Busca veículo por placa
     */
    public VeiculoDTO buscarPorPlaca(String placa) {
        log.info("🔍 Buscando veículo por placa: {}", placa);
        log.debug("🔍 Consultando repositório para placa: {}", placa);
        
        Veiculo veiculo = repository.findByPlaca(placa)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com placa: {}", placa);
                    return new VeiculoNotFoundException("Veículo não encontrado com placa: " + placa);
                });

        log.info("✅ Veículo encontrado - ID: {}, Modelo: {}", veiculo.getId(), veiculo.getModelo());
        log.debug("📝 Veículo completo: {}", veiculo);
        
        return toDTO(veiculo);
    }

    public VeiculoDTO atualizar(Long id, Veiculo dados) {
        log.info("🔄 Atualizando veículo ID: {}", id);
        log.debug("📝 Dados para atualização - Placa: {}, Modelo: {}, Capacidade: {}kg", 
                 dados.getPlaca(), dados.getModelo(), dados.getCapacidadeCarga());
        
        Veiculo veiculo = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com ID: {} para atualização", id);
                    return new VeiculoNotFoundException("Veículo não encontrado com ID: " + id);
                });

        log.debug("📝 Veículo original: {}", veiculo);

        // Verifica se a placa está sendo alterada e se já existe
        if (dados.getPlaca() != null && !dados.getPlaca().equals(veiculo.getPlaca())) {
            if (repository.existsByPlaca(dados.getPlaca())) {
                log.error("❌ Placa já cadastrada: {}", dados.getPlaca());
                throw new VeiculoDuplicateException("Placa '" + dados.getPlaca() + "' já está cadastrada");
            }
            log.debug("📝 Placa alterada: '{}' → '{}'", veiculo.getPlaca(), dados.getPlaca());
            veiculo.setPlaca(dados.getPlaca());
        }

        if (dados.getModelo() != null && !dados.getModelo().equals(veiculo.getModelo())) {
            log.debug("📝 Modelo alterado: '{}' → '{}'", veiculo.getModelo(), dados.getModelo());
            veiculo.setModelo(dados.getModelo());
        }
        
        if (dados.getMarca() != null && !dados.getMarca().equals(veiculo.getMarca())) {
            log.debug("📝 Marca alterada: '{}' → '{}'", veiculo.getMarca(), dados.getMarca());
            veiculo.setMarca(dados.getMarca());
        }
        
        if (dados.getCapacidadeCarga() != null && !dados.getCapacidadeCarga().equals(veiculo.getCapacidadeCarga())) {
            log.debug("📝 Capacidade alterada: {} → {}kg", 
                     veiculo.getCapacidadeCarga(), dados.getCapacidadeCarga());
            veiculo.setCapacidadeCarga(dados.getCapacidadeCarga());
        }
        
        if (dados.getAnoFabricacao() != null && !dados.getAnoFabricacao().equals(veiculo.getAnoFabricacao())) {
            log.debug("📝 Ano alterado: {} → {}", veiculo.getAnoFabricacao(), dados.getAnoFabricacao());
            veiculo.setAnoFabricacao(dados.getAnoFabricacao());
        }
        
        if (dados.getClienteId() != null && !dados.getClienteId().equals(veiculo.getClienteId())) {
            log.debug("📝 Cliente ID alterado: {} → {}", veiculo.getClienteId(), dados.getClienteId());
            veiculo.setClienteId(dados.getClienteId());
        }
        
        if (dados.getMotoristaAtualId() != null && !dados.getMotoristaAtualId().equals(veiculo.getMotoristaAtualId())) {
            log.debug("📝 Motorista atual ID alterado: {} → {}", 
                     veiculo.getMotoristaAtualId(), dados.getMotoristaAtualId());
            veiculo.setMotoristaAtualId(dados.getMotoristaAtualId());
        }

        log.debug("📝 Veículo após modificações: {}", veiculo);
        
        try {
            Veiculo atualizado = repository.save(veiculo);
            log.info("✅ Veículo {} atualizado com sucesso", id);
            log.debug("📝 Veículo após atualização: {}", atualizado);
            
            return toDTO(atualizado);
            
        } catch (DataIntegrityViolationException e) {
            log.error("❌ Erro de integridade ao atualizar veículo {}: {}", id, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Erro ao atualizar veículo");
        }
    }

    public void deletar(Long id) {
        log.info("🗑️ Deletando veículo ID: {}", id);
        
        if (!repository.existsById(id)) {
            log.error("❌ Veículo não encontrado com ID: {} para deleção", id);
            throw new VeiculoNotFoundException("Veículo não encontrado com ID: " + id);
        }
        
        repository.deleteById(id);
        log.info("✅ Veículo {} deletado com sucesso", id);
    }

    // ================ MÉTODOS DE BUSCA ADICIONAIS ================

    /**
     * Busca veículos por modelo (contendo)
     */
    public List<VeiculoDTO> buscarPorModelo(String modelo) {
        log.info("🔍 Buscando veículos com modelo contendo: {}", modelo);
        
        List<Veiculo> veiculos = repository.findByModeloContainingIgnoreCase(modelo);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos com modelo contendo '{}'", dtos.size(), modelo);
        
        return dtos;
    }

    /**
     * Busca veículos por marca
     */
    public List<VeiculoDTO> buscarPorMarca(String marca) {
        log.info("🔍 Buscando veículos da marca: {}", marca);
        
        List<Veiculo> veiculos = repository.findByMarcaIgnoreCase(marca);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos da marca '{}'", dtos.size(), marca);
        
        return dtos;
    }

    /**
     * Busca veículos ativos
     */
    public List<VeiculoDTO> buscarAtivos() {
        log.info("🔍 Buscando veículos ativos");
        
        List<Veiculo> veiculos = repository.findByAtivoTrue();
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos ativos", dtos.size());
        
        return dtos;
    }

    /**
     * Busca veículos por cliente
     */
    public List<VeiculoDTO> buscarPorCliente(Long clienteId) {
        log.info("🔍 Buscando veículos do cliente ID: {}", clienteId);
        
        List<Veiculo> veiculos = repository.findByClienteId(clienteId);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos para o cliente {}", dtos.size(), clienteId);
        
        return dtos;
    }

    /**
     * Busca veículo por placa (retorna Optional)
     */
    public Optional<VeiculoDTO> buscarPorPlacaOptional(String placa) {
        log.debug("🔍 Buscando veículo por placa (optional): {}", placa);
        
        return repository.findByPlaca(placa)
                .map(veiculo -> {
                    log.debug("✅ Veículo encontrado: {}", veiculo.getPlaca());
                    return toDTO(veiculo);
                });
    }

    /**
     * Verifica se placa já existe
     */
    public boolean placaExists(String placa) {
        boolean exists = repository.existsByPlaca(placa);
        log.debug("🔍 Verificando existência da placa '{}': {}", placa, exists);
        return exists;
    }

    // ================ MÉTODOS AUXILIARES ================

    private VeiculoDTO toDTO(Veiculo veiculo) {
        VeiculoDTO dto = new VeiculoDTO(
                veiculo.getId(),
                veiculo.getPlaca(),
                veiculo.getModelo(),
                veiculo.getCapacidadeCarga()
        );
        log.trace("🔄 Convertendo Veículo para DTO - ID: {}, DTO: {}", veiculo.getId(), dto);
        return dto;
    }
}