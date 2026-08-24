package com.telemetria.integration.sefaz.cte.security;

import org.springframework.stereotype.Component;

import com.telemetria.integration.security.XmlSigner;
import com.telemetria.integration.sefaz.cte.exception.CteException;

/** Resolve o certificado CT-e e delega a assinatura XMLDSig ao componente genérico. */
@Component
public class CteXmlSigner {

    private final XmlSigner xmlSigner;

    public CteXmlSigner(XmlSigner xmlSigner) {
        this.xmlSigner = xmlSigner;
    }

    public String assinarXml(String xml, String nomeElemento) {
        try {
            return xmlSigner.assinarXml(xml, nomeElemento);
        } catch (CteException e) {
            throw new CteException("Não foi possível assinar digitalmente o XML do CT-e.", e);
        }
    }
}
