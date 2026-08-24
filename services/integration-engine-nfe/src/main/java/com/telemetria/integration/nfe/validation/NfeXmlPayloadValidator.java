package com.telemetria.integration.nfe.validation;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.telemetria.integration.nfe.domain.exception.NfeException;

/**
 * Valida o limite e a estrutura básica dos XMLs NF-e antes de assinatura ou transporte.
 * A validação XSD completa continua sendo responsabilidade da SEFAZ e do emissor fiscal.
 */
@Component
public class NfeXmlPayloadValidator {

    private final int maxXmlBytes;

    public NfeXmlPayloadValidator(@Value("${sefaz.nfe.max-xml-bytes:1048576}") int maxXmlBytes) {
        this.maxXmlBytes = maxXmlBytes;
    }

    public void validar(String xml, Set<String> raizesPermitidas, String operacao) {
        if (xml == null || xml.isBlank()) {
            throw new NfeException("XML NF-e não pode ser vazio.");
        }
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxXmlBytes) {
            throw new NfeException("XML NF-e excede o limite de " + maxXmlBytes + " bytes.");
        }
        try {
            Document document = criarFactorySegura().newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
            Element raiz = document.getDocumentElement();
            String nomeRaiz = raiz.getLocalName() == null ? raiz.getNodeName() : raiz.getLocalName();
            if (!raizesPermitidas.contains(nomeRaiz)) {
                throw new NfeException("XML incompatível com a operação " + operacao
                        + ": raiz esperada " + raizesPermitidas + ", encontrada <" + nomeRaiz + ">.");
            }
            String versao = raiz.getAttribute("versao");
            if (!versao.isBlank() && !"4.00".equals(versao)) {
                throw new NfeException("A operação " + operacao + " aceita somente XML NF-e versão 4.00.");
            }
        } catch (NfeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NfeException("XML NF-e inválido ou malformado.", exception);
        }
    }

    private DocumentBuilderFactory criarFactorySegura() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }
}
