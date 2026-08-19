package com.telemetria.integration.security;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.telemetria.integration.sefaz.cte.CteException;

/** Valida a assinatura XMLDSig e sua referência ao elemento fiscal esperado. */
@Component
public class XmlSignatureValidator {

    private static final String XMLDSIG_NAMESPACE = XMLSignature.XMLNS;

    public void validar(String xml, String nomeElementoAssinado) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML assinado não pode ser vazio.");
        }

        try {
            Document document = parseXml(xml);
            Element elementoAssinado = localizarElementoUnico(document, nomeElementoAssinado);
            String id = elementoAssinado.getAttribute("Id");
            if (id.isBlank()) {
                throw new CteException("Elemento <" + nomeElementoAssinado + "> não possui atributo Id.");
            }
            elementoAssinado.setIdAttribute("Id", true);

            NodeList assinaturas = document.getElementsByTagNameNS(XMLDSIG_NAMESPACE, "Signature");
            if (assinaturas.getLength() != 1) {
                throw new CteException("O XML deve conter exatamente uma assinatura XMLDSig.");
            }

            DOMValidateContext context = new DOMValidateContext(new EmbeddedKeySelector(), assinaturas.item(0));
            context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
            XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);

            @SuppressWarnings("unchecked")
            List<Reference> references = signature.getSignedInfo().getReferences();
            if (references.size() != 1 || !("#" + id).equals(references.get(0).getURI())) {
                throw new CteException("A assinatura XMLDSig não referencia o elemento <"
                        + nomeElementoAssinado + "> esperado.");
            }
            if (!signature.validate(context)) {
                throw new CteException("Assinatura XMLDSig inválida.");
            }
        } catch (CteException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Não foi possível validar a assinatura XMLDSig.", e);
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Element localizarElementoUnico(Document document, String nomeElemento) {
        NodeList elementos = document.getElementsByTagNameNS("*", nomeElemento);
        if (elementos.getLength() != 1) {
            throw new CteException("O XML deve conter exatamente um elemento <" + nomeElemento + ">.");
        }
        return (Element) elementos.item(0);
    }

    private static final class EmbeddedKeySelector extends KeySelector {
        @Override
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method,
                XMLCryptoContext context) throws KeySelectorException {
            if (keyInfo == null) {
                throw new KeySelectorException("A assinatura XMLDSig não contém KeyInfo.");
            }
            for (XMLStructure structure : keyInfo.getContent()) {
                PublicKey publicKey = extrairChavePublica(structure);
                if (publicKey != null) {
                    Key key = publicKey;
                    return () -> key;
                }
            }
            throw new KeySelectorException("KeyInfo não contém certificado ou chave pública utilizável.");
        }

        private PublicKey extrairChavePublica(XMLStructure structure) throws KeySelectorException {
            try {
                if (structure instanceof KeyValue keyValue) {
                    return keyValue.getPublicKey();
                }
                if (structure instanceof X509Data x509Data) {
                    for (Object item : x509Data.getContent()) {
                        if (item instanceof X509Certificate certificate) {
                            return certificate.getPublicKey();
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                throw new KeySelectorException(e);
            }
        }
    }
}
