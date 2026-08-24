package com.telemetria.integration.nfe.soap;

import org.springframework.stereotype.Component;

import com.telemetria.integration.nfe.util.XmlNfeUtil;
import com.telemetria.integration.nfe.domain.exception.NfeException;

import jakarta.xml.bind.JAXBException;

/**
 * Ponte entre os modelos NF-e gerados a partir dos schemas oficiais e o
 * transporte SOAP do Integration Engine.
 *
 * <p>Esta classe é usada somente para montar consultas sem assinatura. XMLs
 * assinados recebidos pela API não são serializados novamente, pois qualquer
 * alteração invalidaria a assinatura XMLDSig.</p>
 */
@Component
public class NfeSchemaXmlSerializer {

    public String serializar(Object documento) {
        try {
            return XmlNfeUtil.objectToXml(documento);
        } catch (JAXBException | com.telemetria.integration.nfe.exception.ExcecaoNfe exception) {
            throw new NfeException("Não foi possível montar o XML NF-e a partir do schema oficial.", exception);
        }
    }
}
