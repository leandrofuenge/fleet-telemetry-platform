package com.telemetria.integration.nfe;

import javax.xml.bind.JAXBException;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.NfeException;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoGenerico;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoGenerico;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

/**
 * @author Samuel Oliveira - samuel@swconsultoria.com.br Data: 28/09/2017 - 11:11
 */
class EventoGenerico {

	private EventoGenerico(){}

	static TRetEnvEventoGenerico evento(ConfiguracoesNfe config, TEnvEventoGenerico enviEvento, boolean valida)
			throws NfeException {

		try {

			String xml = XmlNfeUtil.objectToXml(enviEvento, config. getEncode());
			xml = xml.replace(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "")
					.replace("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v")
					.replace("<detEvento v", "<detEvento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

			xml = Eventos.enviarEvento(config, xml, ServicosEnum.EVENTO_GENERICO, valida, true, DocumentoEnum.NFE);

			return XmlNfeUtil.xmlToObject(xml, TRetEnvEventoGenerico.class);

		} catch (JAXBException e) {
			throw new NfeException(e.getMessage(),e);
		}

	}

}
