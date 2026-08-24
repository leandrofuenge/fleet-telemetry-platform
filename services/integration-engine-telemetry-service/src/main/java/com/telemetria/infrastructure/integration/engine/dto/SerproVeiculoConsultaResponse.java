package com.telemetria.infrastructure.integration.engine.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SerproVeiculoConsultaResponse(
        Integer code,
        @JsonProperty("code_message") String codeMessage,
        List<VeiculoRadarData> data,
        List<String> errors,
        Boolean sucesso,
        String mensagemErro) {

    public record VeiculoRadarData(String placa, String uf,
            @JsonProperty("marca_modelo") String marcaModelo, List<InfracaoData> infracoes) {
    }

    public record InfracaoData(String ait, String descricao, String data,
            @JsonProperty("data_hora") String dataHora, String hora, String situacao, String autuacao,
            @JsonProperty("autuacao_pdf_url") String autuacaoPdfUrl,
            @JsonProperty("boleto_pdf_url") String boletoPdfUrl) {
    }
}
