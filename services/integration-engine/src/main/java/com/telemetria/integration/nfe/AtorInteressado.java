package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoAtorInteressado;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoAtorInteressado;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

import jakarta.xml.bind.JAXBException;

/**
 * @author Samuel Oliveira - samuel@swconsultoria.com.br
 */
class AtorInteressado {

    static TRetEnvEventoAtorInteressado eventoAtorInteressado(ConfiguracoesNfe config, TEnvEventoAtorInteressado enviEvento, boolean valida)
            throws ExcecaoNfe {

        try {

            String xml = XmlNfeUtil.objectToXml(enviEvento, config.getEncode());
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = xml.replaceAll("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

            xml = Eventos.enviarEvento(config, xml, ServicosEnum.ATOR_INTERESSADO, valida, true, DocumentoEnum.NFE);

            return XmlNfeUtil.xmlToObject(xml, TRetEnvEventoAtorInteressado.class);

        } catch (JAXBException e) {
            throw new ExcecaoNfe(e.getMessage(),e);
        }

    }

}
