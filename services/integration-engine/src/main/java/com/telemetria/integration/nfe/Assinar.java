package com.telemetria.integration.nfe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AssinaturaEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.util.ObjetoUtil;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;

/**
 * Classe Responsavel Por Assinar O Xml.
 *
 */
public class Assinar {

    private static PrivateKey privateKey;
    private static KeyInfo keyInfo;
    Assinar assinarXMLsCertfificadoA1;

    /**
     * @param stringXml
     * @param tipoAssinatura ('NFe' para nfe normal , 'infInut' para inutilizacao, 'evento'
     *                       para eventos)
     * @return String do Xml Assinado
     * @throws ExcecaoNfe
     */
    public static String assinaNfe(ConfiguracoesNfe config, String stringXml, AssinaturaEnum tipoAssinatura) throws ExcecaoNfe {

        stringXml = stringXml.replaceAll("\r\n", "").replaceAll("\n", "").replaceAll(System.lineSeparator(), ""); // Erro quando tem salto de linha.
        stringXml = stringXml.replaceAll("\\s+<", "<"); // Erro Espaço antes do final da Tag.
        stringXml = assinaDocNFe(config, stringXml, tipoAssinatura);
        stringXml = stringXml.replaceAll("&#13;", ""); // Java 11

        return stringXml;
    }

    private static String assinaDocNFe(ConfiguracoesNfe config, String xml, AssinaturaEnum tipoAssinatura) throws ExcecaoNfe {

        try {
            Document document = documentFactory(xml);
            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
            ArrayList<Transform> transformList = signatureFactory(signatureFactory);
            loadCertificates(config, signatureFactory);

            for (int i = 0; i < document.getDocumentElement().getElementsByTagName(tipoAssinatura.getTipo()).getLength(); i++) {
                assinarNFe(tipoAssinatura, signatureFactory, transformList, privateKey, keyInfo, document, i);
            }
            return outputXML(document);
        } catch (SAXException | IOException | ParserConfigurationException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | KeyStoreException | UnrecoverableEntryException
                | CertificadoException | MarshalException
                | XMLSignatureException e) {
            throw new ExcecaoNfe("Erro ao Assinar Nfe" + e.getMessage(),e);
        }
    }

    private static void assinarNFe(AssinaturaEnum tipoAssinatura, XMLSignatureFactory fac, ArrayList<Transform> transformList,
                                   PrivateKey privateKey, KeyInfo ki, Document document, int indexNFe) throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, MarshalException, XMLSignatureException {

        NodeList elements = document.getElementsByTagName(tipoAssinatura.getTag());

        org.w3c.dom.Element el = (org.w3c.dom.Element) elements.item(indexNFe);
        String id = el.getAttribute("Id");
        el.setIdAttribute("Id", true);
        Reference ref = fac.newReference("#" + id, fac.newDigestMethod(DigestMethod.SHA1, null), transformList, null,
                null);

        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));

        XMLSignature signature = fac.newXMLSignature(si, ki);

        DOMSignContext dsc;

        if (tipoAssinatura.equals(AssinaturaEnum.INUTILIZACAO)) {
            dsc = new DOMSignContext(privateKey, document.getFirstChild());
        } else {
            dsc = new DOMSignContext(privateKey,
                    document.getDocumentElement().getElementsByTagName(tipoAssinatura.getTipo()).item(indexNFe));
        }

        dsc.setBaseURI("ok");

        signature.sign(dsc);
    }

    private static ArrayList<Transform> signatureFactory(XMLSignatureFactory signatureFactory)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        ArrayList<Transform> transformList = new ArrayList<Transform>();
        Transform envelopedTransform = signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        Transform c14NTransform = signatureFactory.newTransform("http://www.w3.org/TR/2001/REC-xml-c14n-20010315", (TransformParameterSpec) null);

        transformList.add(envelopedTransform);
        transformList.add(c14NTransform);
        return transformList;
    }

    private static Document documentFactory(String xml) throws SAXException, IOException, ParserConfigurationException {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        return docBuilder.parse(new InputSource(new StringReader(xml)));
    }

    private static void loadCertificates(ConfiguracoesNfe config, XMLSignatureFactory signatureFactory)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException, CertificadoException {

        Certificado certificado = config.getCertificado();
        KeyStore keyStore = CertificadoService.getKeyStore(certificado);

        KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(certificado.getNome(),
                new KeyStore.PasswordProtection(ObjetoUtil.verifica(certificado.getSenha()).orElse("").toCharArray()));
        privateKey = pkEntry.getPrivateKey();

        KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        List<X509Certificate> x509Content = new ArrayList<X509Certificate>();

        x509Content.add(CertificadoService.getCertificate(certificado, keyStore));
        X509Data x509Data = keyInfoFactory.newX509Data(x509Content);
        keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));
    }

    private static String outputXML(Document doc) throws ExcecaoNfe {

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()){
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.transform(new DOMSource(doc), new StreamResult(os));
            String xml = os.toString();
            xml = xml.replaceAll("\\r\\n", "");
            xml = xml.replaceAll(" standalone=\"no\"", "");
            return xml;
        } catch (TransformerException | IOException e) {
            throw new ExcecaoNfe("Erro ao Transformar Documento:" + e.getMessage(),e);
        }
    }
}
