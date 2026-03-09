package com.app.telemetria.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.telemetria.entity.Rota;
import com.app.telemetria.entity.Veiculo;
import com.app.telemetria.exception.BusinessException;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.repository.RotaRepository;
import com.app.telemetria.repository.VeiculoRepository;

@Service
public class RotaService {

    private final RotaRepository repository;
    private final VeiculoRepository veiculoRepository;

    public RotaService(RotaRepository repository, VeiculoRepository veiculoRepository) {
        this.repository = repository;
        this.veiculoRepository = veiculoRepository;
    }

    @Transactional
    public Rota salvar(Rota rota) {

        validarRota(rota);

        if (rota.getStatus() == null) {
            rota.setStatus("PLANEJADA");
        }

        try {
            return repository.save(rota);
        } catch (DataIntegrityViolationException e) {
            throw handleIntegrityException(e, rota.getNome());
        }
    }

    @Transactional(readOnly = true)
    public List<Rota> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Rota buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ROTA_NOT_FOUND,
                        "Rota não encontrada com id: " + id
                ));
    }

    @Transactional
    public Rota atualizar(Long id, Rota dados) {

        Rota rota = buscarPorId(id);

        if (dados.getNome() != null) rota.setNome(dados.getNome());
        if (dados.getOrigem() != null) rota.setOrigem(dados.getOrigem());
        if (dados.getDestino() != null) rota.setDestino(dados.getDestino());
        if (dados.getLatitudeOrigem() != null) rota.setLatitudeOrigem(dados.getLatitudeOrigem());
        if (dados.getLongitudeOrigem() != null) rota.setLongitudeOrigem(dados.getLongitudeOrigem());
        if (dados.getLatitudeDestino() != null) rota.setLatitudeDestino(dados.getLatitudeDestino());
        if (dados.getLongitudeDestino() != null) rota.setLongitudeDestino(dados.getLongitudeDestino());
        if (dados.getDistanciaPrevista() != null) rota.setDistanciaPrevista(dados.getDistanciaPrevista());
        if (dados.getTempoPrevisto() != null) rota.setTempoPrevisto(dados.getTempoPrevisto());
        if (dados.getAtiva() != null) rota.setAtiva(dados.getAtiva());
        if (dados.getDataInicio() != null) rota.setDataInicio(dados.getDataInicio());
        if (dados.getDataFim() != null) rota.setDataFim(dados.getDataFim());

        atualizarStatusSeNecessario(rota, dados.getStatus());

        if (dados.getVeiculo() != null && dados.getVeiculo().getId() != null) {
            Veiculo veiculo = veiculoRepository.findById(dados.getVeiculo().getId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.VEICULO_NOT_FOUND,
                            "Veículo não encontrado com id: " + dados.getVeiculo().getId()
                    ));
            rota.setVeiculo(veiculo);
        }

        try {
            return repository.save(rota);
        } catch (DataIntegrityViolationException e) {
            throw handleIntegrityException(e, dados.getNome());
        }
    }

    @Transactional
    public void deletar(Long id) {
        Rota rota = buscarPorId(id);
        repository.delete(rota);
    }

    // ===============================
    // GERENCIAMENTO DE STATUS
    // ===============================

    @Transactional
    public Rota iniciarRota(Long id) {
        Rota rota = buscarPorId(id);
        rota.setStatus("EM_ANDAMENTO");
        rota.setDataInicio(LocalDateTime.now());
        return repository.save(rota);
    }

    @Transactional
    public Rota finalizarRota(Long id) {
        Rota rota = buscarPorId(id);
        rota.setStatus("FINALIZADA");
        rota.setDataFim(LocalDateTime.now());
        return repository.save(rota);
    }

    @Transactional
    public Rota cancelarRota(Long id) {
        Rota rota = buscarPorId(id);
        rota.setStatus("CANCELADA");
        rota.setDataFim(LocalDateTime.now());
        return repository.save(rota);
    }

    @Transactional(readOnly = true)
    public List<Rota> listarPorStatus(String status) {
        return repository.findByStatus(status);
    }

    // ===============================
    // MÉTODOS PRIVADOS DE SUPORTE
    // ===============================

    private void validarRota(Rota rota) {

        if (rota.getNome() == null || rota.getNome().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "O nome da rota é obrigatório");
        }

        if (rota.getOrigem() == null || rota.getDestino() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Origem e destino são obrigatórios");
        }
    }

    private BusinessException handleIntegrityException(DataIntegrityViolationException e, String nome) {

        String message = e.getMostSpecificCause().getMessage();

        if (message != null && message.toLowerCase().contains("nome")) {
            return new BusinessException(
                    ErrorCode.ROTA_DUPLICATE,
                    "Já existe uma rota com o nome: " + nome
            );
        }

        return new BusinessException(ErrorCode.INTERNAL_ERROR);
    }

    private void atualizarStatusSeNecessario(Rota rota, String novoStatus) {

        if (novoStatus == null) return;

        rota.setStatus(novoStatus);

        switch (novoStatus) {
            case "EM_ANDAMENTO":
                if (rota.getDataInicio() == null) {
                    rota.setDataInicio(LocalDateTime.now());
                }
                break;

            case "FINALIZADA":
            case "CANCELADA":
                if (rota.getDataFim() == null) {
                    rota.setDataFim(LocalDateTime.now());
                }
                break;
        }
    }
}