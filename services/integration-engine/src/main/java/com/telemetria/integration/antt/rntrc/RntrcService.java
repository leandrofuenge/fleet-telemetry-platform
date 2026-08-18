package com.telemetria.integration.antt.rntrc;

import org.springframework.stereotype.Service;

import com.telemetria.integration.sefaz.cte.CteException;

@Service
public class RntrcService {

    private final RntrcClient client;

    public RntrcService(RntrcClient client) {
        this.client = client;
    }

    public RntrcReenvioResponse solicitarAtualizacao(String placa, String documentoTransportador) {
        String placaSanitizada = placa != null ? placa.toUpperCase().replaceAll("[^A-Z0-9]", "") : null;
        String documentoSanitizado = documentoTransportador != null
                ? documentoTransportador.replaceAll("\\D", "")
                : null;
        if ((placaSanitizada == null || placaSanitizada.isBlank())
                && (documentoSanitizado == null || documentoSanitizado.isBlank())) {
            throw new CteException("Placa ou documento do transportador deve ser informado para consulta RNTRC.");
        }

        RntrcReenvioResponse response = client.solicitarReenvioOnDemand(placaSanitizada, documentoSanitizado);
        if (response == null) {
            throw new CteException("Resposta nula recebida da ANTT/RNTRC.");
        }
        return response;
    }

    public boolean validarRegularidade(String placa, String documentoTransportador) {
        return solicitarAtualizacao(placa, documentoTransportador).isSucesso();
    }
}
