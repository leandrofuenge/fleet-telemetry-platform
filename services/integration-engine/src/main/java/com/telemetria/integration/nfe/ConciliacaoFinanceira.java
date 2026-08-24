package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

import jakarta.xml.bind.JAXBException;

/**
 */
class ConciliacaoFinanceira {

    private ConciliacaoFinanceira() {
    }

    static TRetEnvEventoConciliacaoFinanceira eventoEConf(ConfiguracoesNfe config, TEnvEventoConciliacaoFinanceira enviEvento, DocumentoEnum documento, boolean valida)
            throws ExcecaoNfe {

        try {

            String xml = XmlNfeUtil.objectToXml(enviEvento, config.getEncode());
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = xml.replaceAll("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

            xml = Eventos.enviarEvento(config, xml, ServicosEnum.ECONF, valida, true, documento);

            return XmlNfeUtil.xmlToObject(xml, TRetEnvEventoConciliacaoFinanceira.class);

        } catch (JAXBException e) {
            throw new ExcecaoNfe(e.getMessage(),e);
        }

    }

}
