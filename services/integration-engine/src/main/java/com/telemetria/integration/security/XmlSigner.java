package com.telemetria.integration.security;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.certificate.CertificadoLoader;
import com.telemetria.integration.sefaz.cte.CteException;

@Component
public class XmlSigner {

    private final CertificadoLoader certificadoLoader;
    private final SefazProperties sefazProperties;

    public XmlSigner(CertificadoLoader certificadoLoader, SefazProperties sefazProperties) {
        this.certificadoLoader = certificadoLoader;
        this.sefazProperties = sefazProperties;
    }

    public String assinarXml(String xml, String nomeElemento) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML a ser assinado não pode ser vazio.");
        }
        if (nomeElemento == null || nomeElemento.isBlank()) {
            throw new IllegalArgumentException("Nome do elemento a ser assinado é obrigatório.");
        }

        try {
            Document document = parseXml(xml);
            Element elemento = localizarElemento(document, nomeElemento);
            String id = elemento.getAttribute("Id");
            if (id.isBlank()) {
                throw new CteException("Elemento <" + nomeElemento + "> não possui atributo Id.");
            }
            elemento.setIdAttribute("Id", true);

            KeyStore.PrivateKeyEntry certificado = carregarCertificado();
            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
            Reference reference = factory.newReference(
                    "#" + id,
                    factory.newDigestMethod(DigestMethod.SHA256, null),
                    java.util.List.of(
                            factory.newTransform(Transform.ENVELOPED, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null),
                            factory.newTransform(CanonicalizationMethod.INCLUSIVE, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null)
                    ),
                    null,
                    null
            );
            SignedInfo signedInfo = factory.newSignedInfo(
                    factory.newCanonicalizationMethod(
                            CanonicalizationMethod.INCLUSIVE,
                            (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null
                    ),
                    factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    Collections.singletonList(reference)
            );

            X509Certificate x509Certificate = (X509Certificate) certificado.getCertificate();
            KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();
            X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(x509Certificate));
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

            DOMSignContext signContext = new DOMSignContext(certificado.getPrivateKey(), elemento.getParentNode());
            signContext.setDefaultNamespacePrefix("ds");
            XMLSignature signature = factory.newXMLSignature(signedInfo, keyInfo);
            signature.sign(signContext);

            return toXml(document);
        } catch (CteException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Não foi possível assinar digitalmente o XML do CT-e.", e);
        }
    }

    private KeyStore.PrivateKeyEntry carregarCertificado() throws Exception {
        String arquivo = sefazProperties.getCertificado().getArquivo();
        String senha = sefazProperties.getCertificado().getSenha();
        KeyStore keyStore = certificadoLoader.carregarKeyStore(arquivo, senha);
        char[] password = senha != null ? senha.toCharArray() : new char[0];

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Key key = keyStore.getKey(alias, password);
            if (key instanceof PrivateKey && keyStore.getCertificate(alias) instanceof X509Certificate) {
                return new KeyStore.PrivateKeyEntry(
                        (PrivateKey) key,
                        keyStore.getCertificateChain(alias)
                );
            }
        }
        throw new CteException("Certificado A1 não contém uma chave privada utilizável.");
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );
    }

    private Element localizarElemento(Document document, String nomeElemento) {
        NodeList elementos = document.getElementsByTagNameNS("*", nomeElemento);
        if (elementos.getLength() == 0) {
            elementos = document.getElementsByTagName(nomeElemento);
        }
        if (elementos.getLength() == 0) {
            throw new CteException("Elemento <" + nomeElemento + "> não encontrado no XML.");
        }
        return (Element) elementos.item(0);
    }

    private String toXml(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }
}
