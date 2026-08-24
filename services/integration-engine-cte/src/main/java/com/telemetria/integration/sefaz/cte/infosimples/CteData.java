package com.telemetria.integration.sefaz.cte.infosimples;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CteData(
    @JsonProperty("chave_acesso") String chaveAcesso,
    @JsonProperty("numero") String numero,
    @JsonProperty("serie") String serie,
    @JsonProperty("situacao") String situacao,
    @JsonProperty("data_emissao") String dataEmissao,
    @JsonProperty("emitente") ParcialParticipante emitente,
    @JsonProperty("tomador") ParcialParticipante tomador,
    @JsonProperty("rodoviario") RodoviarioData rodoviario,
    @JsonProperty("totais") TotaisData totais,
    @JsonProperty("url_xml") String urlXml,
    @JsonProperty("xml_baixado_com_certificado") String xmlBaixadoBase64
) {}