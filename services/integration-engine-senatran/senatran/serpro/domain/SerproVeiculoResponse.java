package com.telemetria.integration.senatran.serpro.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SerproVeiculoResponse(
        @JsonProperty("code") Integer code,
        @JsonProperty("code_message") String codeMessage,
        @JsonProperty("data") List<VeiculoRadarData> data,
        @JsonProperty("errors") List<String> errors,
        Boolean sucesso,
        String mensagemErro
) {
    public boolean isSucesso() {
        return Boolean.TRUE.equals(sucesso) && (errors == null || errors.isEmpty());
    }

    public static SerproVeiculoResponse erro(String mensagem) {
        return new SerproVeiculoResponse(null, null, null, List.of(mensagem), false, mensagem);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VeiculoRadarData(
            @JsonProperty("placa") String placa,
            @JsonProperty("uf") String uf,
            @JsonProperty("marca_modelo") String marcaModelo,
            @JsonProperty("infracoes") List<InfracaoData> infracoes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InfracaoData(
            @JsonProperty("ait") String ait,
            @JsonProperty("descricao") String descricao,
            @JsonProperty("data") String data,
            @JsonProperty("data_hora") String dataHora,
            @JsonProperty("hora") String hora,
            @JsonProperty("situacao") String situacao,
            @JsonProperty("autuacao") String autuacao,
            @JsonProperty("autuacao_pdf_url") String autuacaoPdfUrl,
            @JsonProperty("boleto_pdf_url") String boletoPdfUrl
    ) {
    }
}
