package com.app.telemetria.service;

import com.app.telemetria.entity.Viagem;
import com.app.telemetria.exception.BusinessException;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.repository.ViagemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ViagemService {

    private final ViagemRepository viagemRepository;

    public ViagemService(ViagemRepository viagemRepository) {
        this.viagemRepository = viagemRepository;
    }

    @Transactional(readOnly = true)
    public List<Viagem> listarTodos() {
        return viagemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Viagem> listarEmAndamento() {
        return viagemRepository.findByStatus("EM_ANDAMENTO");
    }

    @Transactional(readOnly = true)
    public Viagem buscarPorId(Long id) {
        return viagemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.VIAGEM_NOT_FOUND,
                        "Viagem não encontrada com id: " + id
                ));
    }

    @Transactional
    public Viagem salvar(Viagem viagem) {

        validarViagem(viagem);

        if (viagem.getStatus() == null) {
            viagem.setStatus("PLANEJADA");
        }

        return viagemRepository.save(viagem);
    }

    @Transactional
    public Viagem atualizar(Long id, Viagem dados) {

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

        return viagemRepository.save(viagem);
    }

    @Transactional
    public void deletar(Long id) {
        Viagem viagem = buscarPorId(id);
        viagemRepository.delete(viagem);
    }

    @Transactional(readOnly = true)
    public List<Viagem> buscarAtrasadas() {
        return viagemRepository.findAtrasadas(LocalDateTime.now());
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