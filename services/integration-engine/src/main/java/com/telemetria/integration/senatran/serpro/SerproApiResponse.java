package com.telemetria.integration.senatran.serpro;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Contrato bruto da API externa. Não deve vazar para a camada de negócio. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SerproApiResponse(
        Integer code,
        @JsonProperty("code_message") String codeMessage,
        List<VehicleData> data,
        List<String> errors) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VehicleData(
            String placa,
            String uf,
            @JsonProperty("marca_modelo") String marcaModelo,
            List<InfractionData> infracoes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InfractionData(
            String ait, String descricao, String data,
            @JsonProperty("data_hora") String dataHora,
            String hora, String situacao, String autuacao,
            @JsonProperty("autuacao_pdf_url") String autuacaoPdfUrl,
            @JsonProperty("boleto_pdf_url") String boletoPdfUrl) {}
}
