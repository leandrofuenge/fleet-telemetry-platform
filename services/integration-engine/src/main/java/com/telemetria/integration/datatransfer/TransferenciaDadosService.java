package com.telemetria.integration.datatransfer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferenciaDadosService {
    private final TransferenciaDadosRepository repository;

    public TransferenciaDadosService(TransferenciaDadosRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrarSucesso(String correlationId, Base64TransferRequest request, Base64TransferResponse response) {
        repository.save(TransferenciaDados.sucesso(correlationId, request, response, hashDoConteudo(request)));
    }

    @Transactional
    public void registrarFalha(String correlationId, Base64TransferRequest request, Exception exception) {
        repository.save(TransferenciaDados.falha(correlationId, request, hashDoConteudo(request), exception));
    }

    private String hashDoConteudo(Base64TransferRequest request) {
        String conteudo = request.getConteudo() != null ? request.getConteudo() : request.getConteudoBase64();
        if (conteudo == null) conteudo = "";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(64);
            for (byte value : digest) hash.append(String.format("%02x", value));
            return hash.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o hash do conteúdo", exception);
        }
    }
}
