package com.telemetria.integration.nfe;

import javax.xml.bind.JAXBException;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.NfeException;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoManifestacao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoManifestacao;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

/**
 * @author Samuel Oliveira - samuel@swconsultoria.com.br
 * Data: 28/09/2017 - 11:11
 */
class ManifestacaoDestinatario {

	static TRetEnvEventoManifestacao eventoManifestacao(ConfiguracoesNfe config, TEnvEventoManifestacao envEvento , boolean valida) throws NfeException {
		try {

            String xml = XmlNfeUtil.objectToXml(envEvento, config.getEncode());
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = xml.replaceAll("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

            xml = Eventos.enviarEvento(config, xml, ServicosEnum.MANIFESTACAO, valida,true, DocumentoEnum.NFE);

            return XmlNfeUtil.xmlToObject(xml, TRetEnvEventoManifestacao.class);

		} catch (JAXBException e) {
			throw new NfeException(e.getMessage(),e);
		}
	}

}

