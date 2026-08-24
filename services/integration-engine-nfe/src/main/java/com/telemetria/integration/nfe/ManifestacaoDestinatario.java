package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoManifestacao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoManifestacao;

/**
 * Data: 28/09/2017 - 11:11
 */
class ManifestacaoDestinatario {

	static TRetEnvEventoManifestacao eventoManifestacao(ConfiguracoesNfe config, TEnvEventoManifestacao envEvento , boolean valida) throws ExcecaoNfe {
		return EventoNfeSender.enviar(
				config,
				envEvento,
				TRetEnvEventoManifestacao.class,
				ServicosEnum.MANIFESTACAO,
				DocumentoEnum.NFE,
				valida);
	}

}

