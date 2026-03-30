package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Motorista;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.ViagemRepository;

@Service
public class ViagemService {

    private static final Logger log = LoggerFactory.getLogger(ViagemService.class);

    private final ViagemRepository viagemRepository;
    
    @Autowired
    private MotoristaService motoristaService;
    
    @Autowired
    private VeiculoService veiculoService;
    
    @Autowired
    private AlertaService alertaService;

    public ViagemService(ViagemRepository viagemRepository) {
        this.viagemRepository = viagemRepository;
    }

    // ===============================
    // MÉTODOS DE CONSULTA
    // ===============================

    @Transactional(readOnly = true)
    public List<Viagem> listarTodos() {
        log.debug("📋 Listando todas as viagens");
        return viagemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Viagem> listarEmAndamento() {
        log.debug("🛣️ Listando viagens em andamento");
        return viagemRepository.findByStatus("EM_ANDAMENTO");
    }

    @Transactional(readOnly = true)
    public Viagem buscarPorId(Long id) {
        log.debug("🔍 Buscando viagem por ID: {}", id);
        return viagemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.VIAGEM_NOT_FOUND,
                        "Viagem não encontrada com id: " + id
                ));
    }

    @Transactional(readOnly = true)
    public List<Viagem> buscarAtrasadas() {
        log.debug("⏰ Buscando viagens atrasadas");
        return viagemRepository.findAtrasadas(LocalDateTime.now());
    }

    // ===============================
    // MÉTODOS CRUD
    // ===============================

    @Transactional
    public Viagem salvar(Viagem viagem) {
        log.info("➕ Salvando nova viagem");
        validarViagem(viagem);

        if (viagem.getStatus() == null) {
            viagem.setStatus("PLANEJADA");
        }

        Viagem salva = viagemRepository.save(viagem);
        log.info("✅ Viagem salva com ID: {}", salva.getId());
        return salva;
    }

    @Transactional
    public Viagem atualizar(Long id, Viagem dados) {
        log.info("🔄 Atualizando viagem ID: {}", id);
        Viagem viagem = buscarPorId(id);

        if (dados.getVeiculo() != null) viagem.setVeiculo(dados.getVeiculo());
        if (dados.getMotorista() != null) viagem.setMotorista(dados.getMotorista());
        if (dados.getCarga() != null) viagem.setCarga(dados.getCarga());
        if (dados.getRota() != null) viagem.setRota(dados.getRota());
        if (dados.getDataSaida() != null) viagem.setDataSaida(dados.getDataSaida());
        if (dados.getDataChegadaPrevista() != null) viagem.setDataChegadaPrevista(dados.getDataChegadaPrevista());
        if (dados.getDataChegadaReal() != null) viagem.setDataChegadaReal(dados.getDataChegadaReal());
        if (dados.getObservacoes() != null) viagem.setObservacoes(dados.getObservacoes());

        atualizarStatusSeNecessario(viagem, dados.getStatus());

        Viagem atualizada = viagemRepository.save(viagem);
        log.info("✅ Viagem {} atualizada com sucesso", id);
        return atualizada;
    }

    @Transactional
    public void deletar(Long id) {
        log.info("🗑️ Deletando viagem ID: {}", id);
        Viagem viagem = buscarPorId(id);
        viagemRepository.delete(viagem);
        log.info("✅ Viagem {} deletada com sucesso", id);
    }

    // ===============================
    // MÉTODOS DE INÍCIO DE VIAGEM (COM VALIDAÇÕES)
    // ===============================

    /**
     * Inicia uma viagem após validar todas as regras de negócio
     * RN-VEI-003: Documentos do veículo
     * RN-MOT-002: CNH válida e categoria compatível
     * RN-MOT-003: ASO válido e MOPP para carga perigosa
     * RN-MOT-004: Score do motorista
     */
    @Transactional
    public void iniciarViagem(Long viagemId) {
        log.info("🚀 Iniciando viagem ID: {}", viagemId);
        
        Viagem viagem = buscarPorId(viagemId);
        
        // Validar se a viagem já está em andamento
        if ("EM_ANDAMENTO".equals(viagem.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Viagem já está em andamento");
        }
        
        // Validar se a viagem já foi finalizada
        if ("FINALIZADA".equals(viagem.getStatus()) || "CANCELADA".equals(viagem.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Viagem já foi " + viagem.getStatus().toLowerCase() + ". Não pode ser reiniciada");
        }
        
        // Obter motorista e veículo
        Motorista motorista = viagem.getMotorista();
        Veiculo veiculo = viagem.getVeiculo();
        
        if (motorista == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Motorista não definido para esta viagem");
        }
        
        if (veiculo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Veículo não definido para esta viagem");
        }
        
        // ===== RN-MOT-002: Validações da CNH =====
        motoristaService.validarCnhParaViagem(motorista);
        motoristaService.validarCategoriaCnhParaVeiculo(motorista, veiculo);
        
        // ===== RN-MOT-003: Validações de ASO e MOPP =====
        motoristaService.validarAsoParaViagem(motorista);
        
        // Validação de MOPP para carga perigosa
        if (viagem.getCarga() != null) {
            motoristaService.validarMoppingParaCargaPerigosa(motorista, viagem.getCarga());
        }
        
        // ===== RN-MOT-004: Validação de Score =====
        motoristaService.validarScoreParaViagem(motorista);
        
        // ===== RN-VEI-003: Validar documentos do veículo =====
        veiculoService.validarDocumentosParaViagem(veiculo.getId());
        
        // Atualizar status da viagem
        viagem.setStatus("EM_ANDAMENTO");
        viagem.setDataSaida(LocalDateTime.now());
        viagemRepository.save(viagem);
        
        log.info("✅ Viagem {} iniciada com sucesso. Motorista: {}, Veículo: {}", 
                viagemId, motorista.getNome(), veiculo.getPlaca());
    }

    // ===============================
    // MÉTODOS DE FINALIZAÇÃO DE VIAGEM
    // ===============================

    /**
     * Finaliza uma viagem em andamento
     */
    @Transactional
    public void finalizarViagem(Long viagemId) {
        log.info("🏁 Finalizando viagem ID: {}", viagemId);
        
        Viagem viagem = buscarPorId(viagemId);
        
        if (!"EM_ANDAMENTO".equals(viagem.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Apenas viagens em andamento podem ser finalizadas. Status atual: " + viagem.getStatus());
        }
        
        viagem.setStatus("FINALIZADA");
        viagem.setDataChegadaReal(LocalDateTime.now());
        viagemRepository.save(viagem);
        
        log.info("✅ Viagem {} finalizada com sucesso", viagemId);
    }

    /**
     * Cancela uma viagem
     */
    @Transactional
    public void cancelarViagem(Long viagemId, String motivo) {
        log.info("❌ Cancelando viagem ID: {}, Motivo: {}", viagemId, motivo);
        
        Viagem viagem = buscarPorId(viagemId);
        
        if ("FINALIZADA".equals(viagem.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Viagem já finalizada não pode ser cancelada");
        }
        
        viagem.setStatus("CANCELADA");
        viagem.setObservacoes("CANCELADA: " + motivo);
        viagemRepository.save(viagem);
        
        log.info("✅ Viagem {} cancelada com sucesso", viagemId);
    }

    // ===============================
    // MÉTODOS PRIVADOS DE SUPORTE
    // ===============================

    private void validarViagem(Viagem viagem) {
        if (viagem.getVeiculo() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Veículo é obrigatório para a viagem");
        }

        if (viagem.getMotorista() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Motorista é obrigatório para a viagem");
        }

        if (viagem.getRota() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Rota é obrigatória para a viagem");
        }

        if (viagem.getDataSaida() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Data de saída é obrigatória");
        }
    }

    private void atualizarStatusSeNecessario(Viagem viagem, String novoStatus) {
        if (novoStatus == null) return;

        viagem.setStatus(novoStatus);

        switch (novoStatus) {
            case "EM_ANDAMENTO":
                if (viagem.getDataSaida() == null) {
                    viagem.setDataSaida(LocalDateTime.now());
                }
                break;

            case "FINALIZADA":
            case "CANCELADA":
                if (viagem.getDataChegadaReal() == null) {
                    viagem.setDataChegadaReal(LocalDateTime.now());
                }
                break;
        }
    }
}