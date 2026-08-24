package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoConciliacaoFinanceira;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

import jakarta.xml.bind.JAXBException;

/**
 */
class CancConciliacaoFinanceira {

    private CancConciliacaoFinanceira() {
    }

    static TRetEnvEventoCancelamentoConciliacaoFinanceira eventoEConf(ConfiguracoesNfe config, TEnvEventoCancelamentoConciliacaoFinanceira enviEvento, boolean valida)
            throws ExcecaoNfe {

        try {

            String xml = XmlNfeUtil.objectToXml(enviEvento, config.getEncode());
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = xml.replaceAll("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

            xml = Eventos.enviarEvento(config, xml, ServicosEnum.CANC_ECONF, valida, true, DocumentoEnum.NFE);

            return XmlNfeUtil.xmlToObject(xml, TRetEnvEventoCancelamentoConciliacaoFinanceira.class);

        } catch (JAXBException e) {
            throw new ExcecaoNfe(e.getMessage(),e);
        }

    }

}
