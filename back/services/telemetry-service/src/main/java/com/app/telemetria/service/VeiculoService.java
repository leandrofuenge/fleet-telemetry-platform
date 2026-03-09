package com.app.telemetria.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.telemetria.dto.VeiculoDTO;
import com.app.telemetria.entity.Veiculo;
import com.app.telemetria.repository.VeiculoRepository;

@Service
public class VeiculoService {

    @Autowired
    private VeiculoRepository repository;

    public VeiculoDTO salvar(Veiculo veiculo) {
        Veiculo salvo = repository.save(veiculo);
        return toDTO(salvo);
    }

    public List<VeiculoDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public VeiculoDTO buscarPorId(Long id) {
        Veiculo veiculo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

        return toDTO(veiculo);
    }

    public VeiculoDTO atualizar(Long id, Veiculo dados) {
        Veiculo veiculo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

        veiculo.setPlaca(dados.getPlaca());
        veiculo.setModelo(dados.getModelo());
        veiculo.setCapacidadeCarga(dados.getCapacidadeCarga());

        Veiculo atualizado = repository.save(veiculo);

        return toDTO(atualizado);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    private VeiculoDTO toDTO(Veiculo veiculo) {
        return new VeiculoDTO(
                veiculo.getId(),
                veiculo.getPlaca(),
                veiculo.getModelo(),
                veiculo.getCapacidadeCarga()
        );
    }
}
