package com.telemetria.integration.sefaz.cte.infosimples;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InfosimplesCteRequest(
	    @JsonProperty("cte") String chaveCte,
	    @JsonProperty("pkcs12_cert") String certificadoBase64,
	    @JsonProperty("pkcs12_pass") String senhaCertificado
	) {}