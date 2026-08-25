package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoGenerico;
import com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoGenerico;

/**
 */
class EventoGenerico {

	private EventoGenerico(){}

	static TRetEnvEventoGenerico evento(ConfiguracoesNfe config, TEnvEventoGenerico enviEvento, boolean valida)
			throws ExcecaoNfe {

		return EventoNfeSender.enviar(
				config,
				enviEvento,
				TRetEnvEventoGenerico.class,
				ServicosEnum.EVENTO_GENERICO,
				DocumentoEnum.NFE,
				valida,
				xml -> xml.replace("<detEvento v", "<detEvento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v"));

	}

}
