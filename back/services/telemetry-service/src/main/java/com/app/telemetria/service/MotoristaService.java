package com.app.telemetria.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.app.telemetria.entity.Motorista;
import com.app.telemetria.exception.BusinessException;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.repository.MotoristaRepository;

@Service
public class MotoristaService {

    @Autowired
    private MotoristaRepository repository;

    public Motorista salvar(Motorista motorista) {
        try {
            return repository.save(motorista);
        } catch (DataIntegrityViolationException e) {

            String message = e.getMostSpecificCause().getMessage();

            if (message != null) {
                String lowerMessage = message.toLowerCase();

                if (lowerMessage.contains("cpf")) {
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com o CPF: " + motorista.getCpf()
                    );
                }

                if (lowerMessage.contains("cnh")) {
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com a CNH: " + motorista.getCnh()
                    );
                }
            }

            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public List<Motorista> listar() {
        return repository.findAll();
    }

    public Motorista buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MOTORISTA_NOT_FOUND,
                        "Motorista não encontrado com id: " + id
                ));
    }

    public Motorista buscarPorCpf(String cpf) {
        return repository.findByCpf(cpf)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MOTORISTA_NOT_FOUND,
                        "Motorista não encontrado com CPF: " + cpf
                ));
    }

    public Motorista atualizar(Long id, Motorista dados) {

        Motorista motorista = buscarPorId(id);

        motorista.setNome(dados.getNome());
        motorista.setCpf(dados.getCpf());
        motorista.setCnh(dados.getCnh());
        motorista.setCategoriaCnh(dados.getCategoriaCnh());

        try {
            return repository.save(motorista);
        } catch (DataIntegrityViolationException e) {

            String message = e.getMostSpecificCause().getMessage();

            if (message != null) {
                String lowerMessage = message.toLowerCase();

                if (lowerMessage.contains("cpf")) {
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com o CPF: " + dados.getCpf()
                    );
                }

                if (lowerMessage.contains("cnh")) {
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com a CNH: " + dados.getCnh()
                    );
                }
            }

            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public void deletar(Long id) {
        Motorista motorista = buscarPorId(id);
        repository.delete(motorista);
    }
}