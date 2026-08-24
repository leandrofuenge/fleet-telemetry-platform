package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamento;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamento;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

import jakarta.xml.bind.JAXBException;

/**
 */
class Cancelar {

    static TRetEnvEventoCancelamento eventoCancelamento(ConfiguracoesNfe config, TEnvEventoCancelamento enviEvento, boolean valida, DocumentoEnum tipoDocumento)
            throws ExcecaoNfe {

        try {

            String xml = XmlNfeUtil.objectToXml(enviEvento, config.getEncode());
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = xml.replaceAll("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

            xml = Eventos.enviarEvento(config, xml, ServicosEnum.CANCELAMENTO, valida, true, tipoDocumento);

            return XmlNfeUtil.xmlToObject(xml, TRetEnvEventoCancelamento.class);

        } catch (JAXBException e) {
            throw new ExcecaoNfe(e.getMessage(), e);
        }

    }

    static com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoSubstituicao eventoCancelamentoSubstituicao(ConfiguracoesNfe config, com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoSubstituicao enviEvento, boolean valida)
            throws ExcecaoNfe {

        try {

            String xml = XmlNfeUtil.objectToXml(enviEvento, config.getEncode());
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = xml.replaceAll("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

            xml = Eventos.enviarEvento(config, xml, ServicosEnum.CANCELAMENTO_SUBSTITUICAO, valida, true, DocumentoEnum.NFCE);

            return XmlNfeUtil.xmlToObject(xml, com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoSubstituicao.class);

        } catch (JAXBException e) {
            throw new ExcecaoNfe(e.getMessage(), e);
        }

    }

}
