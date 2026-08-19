package com.telemetria.integration.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.telemetria.integration.sefaz.cte.CteException;

class XmlSignatureValidatorTests {

    private final XmlSignatureValidator validator = new XmlSignatureValidator();

    @Test
    void deveAceitarAssinaturaCriptograficamenteValida() throws Exception {
        assertDoesNotThrow(() -> validator.validar(assinar("ID123"), "infCte"));
    }

    @Test
    void deveRejeitarXmlSemAssinatura() {
        assertThrows(CteException.class,
                () -> validator.validar("<CTe><infCte Id=\"ID123\"/></CTe>", "infCte"));
    }

    @Test
    void deveRejeitarConteudoAlteradoDepoisDaAssinatura() throws Exception {
        String adulterado = assinar("ID123").replace("valor-original", "valor-alterado");
        assertThrows(CteException.class, () -> validator.validar(adulterado, "infCte"));
    }

    private String assinar(String id) throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
        Document document = documentFactory.newDocumentBuilder().parse(new ByteArrayInputStream(
                ("<CTe><infCte Id=\"" + id + "\"><xNome>valor-original</xNome></infCte></CTe>")
                        .getBytes(StandardCharsets.UTF_8)));
        Element infCte = (Element) document.getElementsByTagName("infCte").item(0);
        infCte.setIdAttribute("Id", true);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        Reference reference = factory.newReference("#" + id,
                factory.newDigestMethod(DigestMethod.SHA256, null),
                List.of(factory.newTransform(Transform.ENVELOPED,
                                (javax.xml.crypto.dsig.spec.TransformParameterSpec) null),
                        factory.newTransform(CanonicalizationMethod.INCLUSIVE,
                                (javax.xml.crypto.dsig.spec.TransformParameterSpec) null)), null, null);
        var signedInfo = factory.newSignedInfo(
                factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                        (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null),
                factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null), List.of(reference));
        KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();
        KeyInfo keyInfo = keyInfoFactory.newKeyInfo(List.of(keyInfoFactory.newKeyValue(keyPair.getPublic())));
        DOMSignContext context = new DOMSignContext(keyPair.getPrivate(), infCte.getParentNode());
        context.setDefaultNamespacePrefix("ds");
        XMLSignature signature = factory.newXMLSignature(signedInfo, keyInfo);
        signature.sign(context);

        StringWriter writer = new StringWriter();
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }
}
