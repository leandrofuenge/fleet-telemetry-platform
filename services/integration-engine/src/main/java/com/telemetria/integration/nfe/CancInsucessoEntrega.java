package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoInsucessoEntrega;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoInsucessoEntrega;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

import jakarta.xml.bind.JAXBException;

/**
 * @author Samuel Oliveira - samuel@swconsultoria.com.br
 */
class CancInsucessoEntrega {

    private CancInsucessoEntrega() {
    }

    static TRetEnvEventoCancelamentoInsucessoEntrega eventoCancInsuccessoEntrega(ConfiguracoesNfe config, TEnvEventoCancelamentoInsucessoEntrega enviEvento, boolean valida)
            throws ExcecaoNfe {

        try {

            String xml = XmlNfeUtil.objectToXml(enviEvento, config.getEncode());
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = xml.replaceAll("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

            xml = Eventos.enviarEvento(config, xml, ServicosEnum.CANC_INSUCESSO_ENTREGA, valida, true, DocumentoEnum.NFE);

            return XmlNfeUtil.xmlToObject(xml, TRetEnvEventoCancelamentoInsucessoEntrega.class);

        } catch (JAXBException e) {
            throw new ExcecaoNfe(e.getMessage(), e);
        }

    }

}
