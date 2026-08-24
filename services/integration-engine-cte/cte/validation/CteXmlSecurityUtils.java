package com.telemetria.integration.sefaz.cte.validation;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.telemetria.integration.sefaz.cte.exception.CteException;

public final class CteXmlSecurityUtils {

    private CteXmlSecurityUtils() {}

    /**
     * Cria uma fábrica DocumentBuilder configurada contra vulnerabilidades XXE.
     */
    public static DocumentBuilderFactory createSecureDocumentBuilderFactory() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            dbf.setNamespaceAware(true);
        } catch (Exception e) {
            throw new CteException("Falha ao inicializar configurações de segurança XML.", e);
        }
        return dbf;
    }
}
