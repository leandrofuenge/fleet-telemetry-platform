package com.telemetria.integration.nfe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.AssinaturaEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.util.ObjetoUtil;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;

/**
 * Classe responsável por assinar XML da NF-e.
 */
public final class Assinar {

    /**
     * Namespace oficial da NF-e.
     */
    private static final String NFE_NAMESPACE =
            "http://www.portalfiscal.inf.br/nfe";

    /**
     * Algoritmo de digest utilizado na assinatura.
     *
     * IMPORTANTE:
     * Não alterar sem validar o padrão da NF-e/SEFAZ utilizado
     * pela aplicação.
     */
    private static final String DIGEST_ALGORITHM =
            DigestMethod.SHA1;

    /**
     * Algoritmo de assinatura utilizado.
     *
     * IMPORTANTE:
     * Não alterar sem validar o padrão da NF-e/SEFAZ utilizado
     * pela aplicação.
     */
    private static final String SIGNATURE_ALGORITHM =
            SignatureMethod.RSA_SHA1;

    /**
     * Método de canonicalização.
     */
    private static final String CANONICALIZATION_ALGORITHM =
            CanonicalizationMethod.INCLUSIVE;

    /**
     * Transform de canonicalização XML.
     */
    private static final String C14N_TRANSFORM =
            "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    private Assinar() {
        // Classe utilitária.
    }

    /**
     * Assina o XML da NF-e.
     *
     * @param config configurações da NF-e
     * @param stringXml XML que será assinado
     * @param tipoAssinatura tipo de assinatura
     * @return XML assinado
     * @throws ExcecaoNfe caso ocorra algum erro durante a assinatura
     */
    public static String assinaNfe(
            ConfiguracoesNfe config,
            String stringXml,
            AssinaturaEnum tipoAssinatura) throws ExcecaoNfe {

        validarParametros(
                config,
                stringXml,
                tipoAssinatura
        );

        String xmlAssinado = assinaDocNFe(
                config,
                stringXml,
                tipoAssinatura
        );

        /*
         * Mantido por compatibilidade com a implementação anterior.
         */
        return xmlAssinado.replace("&#13;", "");
    }

    /**
     * Valida os parâmetros de entrada.
     */
    private static void validarParametros(
            ConfiguracoesNfe config,
            String xml,
            AssinaturaEnum tipoAssinatura)
            throws ExcecaoNfe {

        if (config == null) {
            throw new ExcecaoNfe(
                    "Configurações da NFe não informadas."
            );
        }

        if (xml == null || xml.isBlank()) {
            throw new ExcecaoNfe(
                    "XML não informado."
            );
        }

        if (tipoAssinatura == null) {
            throw new ExcecaoNfe(
                    "Tipo de assinatura não informado."
            );
        }

        if (config.getCertificado() == null) {
            throw new ExcecaoNfe(
                    "Certificado digital não configurado."
            );
        }
    }

    /**
     * Executa a assinatura do documento.
     */
    private static String assinaDocNFe(
            ConfiguracoesNfe config,
            String xml,
            AssinaturaEnum tipoAssinatura)
            throws ExcecaoNfe {

        try {

            /*
             * Cria a fábrica de assinatura.
             */
            XMLSignatureFactory signatureFactory =
                    XMLSignatureFactory.getInstance("DOM");

            /*
             * Carrega certificado e chave privada.
             *
             * Os dados são locais à execução.
             * Não utilizamos campos static para evitar
             * problemas de concorrência.
             */
            DadosCertificado dadosCertificado =
                    loadCertificates(
                            config,
                            signatureFactory
                    );

            /*
             * Cria as transformações utilizadas
             * na assinatura.
             */
            ArrayList<Transform> transformList =
                    createTransforms(signatureFactory);

            /*
             * Converte o XML para DOM.
             */
            Document document =
                    documentFactory(xml);

            /*
             * Localiza os elementos que precisam
             * ser assinados.
             */
            NodeList elements =
                    getElementsToSign(
                            document,
                            tipoAssinatura
                    );

            /*
             * Valida os elementos antes de modificar
             * o documento.
             */
            validarElementosParaAssinatura(
                    elements,
                    tipoAssinatura
            );

            /*
             * Executa as assinaturas.
             */
            for (int i = 0; i < elements.getLength(); i++) {

                assinarNFe(
                        tipoAssinatura,
                        signatureFactory,
                        transformList,
                        dadosCertificado.privateKey(),
                        dadosCertificado.keyInfo(),
                        document,
                        elements,
                        i
                );
            }

            /*
             * Converte o DOM assinado novamente
             * para String.
             */
            return outputXML(document);

        } catch (SAXException
                 | IOException
                 | ParserConfigurationException
                 | NoSuchAlgorithmException
                 | InvalidAlgorithmParameterException
                 | KeyStoreException
                 | UnrecoverableEntryException
                 | CertificadoException
                 | MarshalException
                 | XMLSignatureException e) {

            throw new ExcecaoNfe(
                    "Erro ao assinar NFe: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Valida os elementos que serão assinados.
     *
     * Verifica:
     * - existência de elementos;
     * - se são elementos XML válidos;
     * - existência do atributo Id;
     * - IDs duplicados.
     */
    private static void validarElementosParaAssinatura(
            NodeList elements,
            AssinaturaEnum tipoAssinatura)
            throws ExcecaoNfe {

        if (elements == null || elements.getLength() == 0) {
            throw new ExcecaoNfe(
                    "Nenhum elemento encontrado para assinatura: "
                            + tipoAssinatura.getTag()
            );
        }

        Set<String> ids =
                new HashSet<>();

        for (int i = 0; i < elements.getLength(); i++) {

            Node node = elements.item(i);

            if (!(node instanceof Element)) {
                throw new ExcecaoNfe(
                        "Elemento inválido encontrado para assinatura. "
                                + "Índice: " + i
                );
            }

            Element element =
                    (Element) node;

            String id =
                    element.getAttribute("Id");

            if (id == null || id.isBlank()) {
                throw new ExcecaoNfe(
                        "O elemento "
                                + tipoAssinatura.getTag()
                                + " na posição "
                                + i
                                + " não possui o atributo Id."
                );
            }

            /*
             * Verifica IDs duplicados.
             */
            if (!ids.add(id)) {
                throw new ExcecaoNfe(
                        "ID duplicado encontrado no XML: "
                                + id
                );
            }
        }
    }

    /**
     * Assina um elemento específico do documento.
     */
    private static void assinarNFe(
            AssinaturaEnum tipoAssinatura,
            XMLSignatureFactory fac,
            ArrayList<Transform> transformList,
            PrivateKey privateKey,
            KeyInfo keyInfo,
            Document document,
            NodeList elements,
            int indexNFe)
            throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            MarshalException,
            XMLSignatureException {

        Node node =
                elements.item(indexNFe);

        if (!(node instanceof Element)) {
            throw new XMLSignatureException(
                    "Elemento inválido para assinatura. "
                            + "Índice: "
                            + indexNFe
            );
        }

        Element element =
                (Element) node;

        String id =
                element.getAttribute("Id");

        if (id == null || id.isBlank()) {
            throw new XMLSignatureException(
                    "Elemento "
                            + tipoAssinatura.getTag()
                            + " não possui atributo Id."
            );
        }

        /*
         * Informa ao DOM que o atributo Id deve
         * ser tratado como identificador.
         */
        element.setIdAttribute(
                "Id",
                true
        );

        /*
         * Cria a referência para o elemento.
         */
        Reference reference =
                fac.newReference(
                        "#" + id,
                        fac.newDigestMethod(
                                DIGEST_ALGORITHM,
                                null
                        ),
                        transformList,
                        null,
                        null
                );

        /*
         * Cria as informações assinadas.
         */
        SignedInfo signedInfo =
                fac.newSignedInfo(
                        fac.newCanonicalizationMethod(
                                CANONICALIZATION_ALGORITHM,
                                (C14NMethodParameterSpec) null
                        ),
                        fac.newSignatureMethod(
                                SIGNATURE_ALGORITHM,
                                null
                        ),
                        Collections.singletonList(
                                reference
                        )
                );

        /*
         * Cria a assinatura XML.
         */
        XMLSignature signature =
                fac.newXMLSignature(
                        signedInfo,
                        keyInfo
                );

        DOMSignContext signContext;

        /*
         * Inutilização possui comportamento diferente
         * em relação ao nó onde a assinatura é inserida.
         */
        if (tipoAssinatura ==
                AssinaturaEnum.INUTILIZACAO) {

            signContext =
                    new DOMSignContext(
                            privateKey,
                            document.getDocumentElement()
                    );

        } else {

            signContext =
                    new DOMSignContext(
                            privateKey,
                            element
                    );
        }

        /*
         * Executa a assinatura.
         */
        signature.sign(signContext);
    }

    /**
     * Cria as transformações utilizadas na assinatura.
     */
    private static ArrayList<Transform> createTransforms(
            XMLSignatureFactory signatureFactory)
            throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {

        ArrayList<Transform> transformList =
                new ArrayList<>();

        /*
         * Transform enveloped.
         */
        Transform envelopedTransform =
                signatureFactory.newTransform(
                        Transform.ENVELOPED,
                        (TransformParameterSpec) null
                );

        /*
         * Transform de canonicalização.
         */
        Transform c14nTransform =
                signatureFactory.newTransform(
                        C14N_TRANSFORM,
                        (TransformParameterSpec) null
                );

        transformList.add(
                envelopedTransform
        );

        transformList.add(
                c14nTransform
        );

        return transformList;
    }

    /**
     * Cria o DOM do XML.
     *
     * O parser é configurado para evitar XXE
     * e carregamento de recursos externos.
     */
    private static Document documentFactory(
            String xml)
            throws SAXException,
            IOException,
            ParserConfigurationException {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        /*
         * Necessário para trabalhar corretamente
         * com namespaces XML.
         */
        factory.setNamespaceAware(true);

        /*
         * Bloqueia DOCTYPE.
         */
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        /*
         * Bloqueia entidades externas.
         */
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        /*
         * Impede carregamento de DTD externo.
         */
        factory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
        );

        /*
         * Desabilita XInclude.
         */
        factory.setXIncludeAware(false);

        /*
         * Não expande entidades.
         */
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder =
                factory.newDocumentBuilder();

        return builder.parse(
                new InputSource(
                        new StringReader(xml)
                )
        );
    }

    /**
     * Localiza os elementos que devem ser assinados.
     *
     * Primeiro tenta encontrar pelo namespace oficial
     * da NF-e. Caso não encontre, utiliza busca pelo nome
     * para manter compatibilidade com XMLs existentes.
     */
    private static NodeList getElementsToSign(
            Document document,
            AssinaturaEnum tipoAssinatura) {

        String tag =
                tipoAssinatura.getTag();

        NodeList elements =
                document.getElementsByTagNameNS(
                        NFE_NAMESPACE,
                        tag
                );

        /*
         * Fallback para manter compatibilidade com XMLs
         * que não estejam utilizando namespace corretamente.
         */
        if (elements.getLength() == 0) {
            elements =
                    document.getElementsByTagName(
                            tag
                    );
        }

        return elements;
    }

    /**
     * Carrega o certificado digital e a chave privada.
     */
    private static DadosCertificado loadCertificates(
            ConfiguracoesNfe config,
            XMLSignatureFactory signatureFactory)
            throws KeyStoreException,
            NoSuchAlgorithmException,
            UnrecoverableEntryException,
            CertificadoException {

        Certificado certificado =
                config.getCertificado();

        if (certificado == null) {
            throw new CertificadoException(
                    "Certificado digital não configurado."
            );
        }

        /*
         * Carrega o KeyStore.
         */
        KeyStore keyStore =
                CertificadoService.getKeyStore(
                        certificado
                );

        /*
         * Obtém a senha do certificado.
         */
        String senha =
                ObjetoUtil.verifica(
                        certificado.getSenha()
                ).orElse("");

        /*
         * Obtém a entrada da chave privada.
         */
        KeyStore.PrivateKeyEntry pkEntry =
                (KeyStore.PrivateKeyEntry)
                        keyStore.getEntry(
                                certificado.getNome(),
                                new KeyStore.PasswordProtection(
                                        senha.toCharArray()
                                )
                        );

        if (pkEntry == null) {
            throw new KeyStoreException(
                    "Não foi possível obter a chave privada "
                            + "do certificado."
            );
        }

        /*
         * Obtém a chave privada.
         */
        PrivateKey privateKey =
                pkEntry.getPrivateKey();

        if (privateKey == null) {
            throw new KeyStoreException(
                    "A chave privada do certificado "
                            + "não foi encontrada."
            );
        }

        /*
         * Obtém o certificado X509.
         */
        X509Certificate certificate =
                CertificadoService.getCertificate(
                        certificado,
                        keyStore
                );

        if (certificate == null) {
            throw new KeyStoreException(
                    "Certificado X509 não encontrado."
            );
        }

        /*
         * Cria o KeyInfo.
         */
        KeyInfoFactory keyInfoFactory =
                signatureFactory.getKeyInfoFactory();

        List<X509Certificate> x509Content =
                new ArrayList<>();

        x509Content.add(
                certificate
        );

        X509Data x509Data =
                keyInfoFactory.newX509Data(
                        x509Content
                );

        KeyInfo keyInfo =
                keyInfoFactory.newKeyInfo(
                        Collections.singletonList(
                                x509Data
                        )
                );

        return new DadosCertificado(
                privateKey,
                keyInfo
        );
    }

    /**
     * Converte o DOM para String XML.
     */
    private static String outputXML(
            Document doc) throws ExcecaoNfe {

        try (ByteArrayOutputStream os =
                     new ByteArrayOutputStream()) {

            TransformerFactory transformerFactory =
                    TransformerFactory.newInstance();

            Transformer transformer =
                    transformerFactory.newTransformer();

            /*
             * Define UTF-8 explicitamente.
             */
            transformer.setOutputProperty(
                    OutputKeys.ENCODING,
                    StandardCharsets.UTF_8.name()
            );

            /*
             * Mantém a declaração XML.
             */
            transformer.setOutputProperty(
                    OutputKeys.OMIT_XML_DECLARATION,
                    "no"
            );

            transformer.transform(
                    new DOMSource(doc),
                    new StreamResult(os)
            );

            String xml =
                    os.toString(
                            StandardCharsets.UTF_8
                    );

            /*
             * Mantido por compatibilidade com a
             * implementação original.
             */
            xml = xml.replace(
                    " standalone=\"no\"",
                    ""
            );

            /*
             * Mantido por compatibilidade com a
             * implementação original.
             */
            xml = xml.replace(
                    "\r\n",
                    ""
            );

            return xml;

        } catch (TransformerException
                 | IOException e) {

            throw new ExcecaoNfe(
                    "Erro ao transformar documento: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Objeto contendo os dados necessários
     * para realizar a assinatura.
     *
     * Não utilizamos campos static para PrivateKey
     * e KeyInfo, evitando problemas de concorrência
     * em aplicações web.
     */
    private record DadosCertificado(
            PrivateKey privateKey,
            KeyInfo keyInfo) {
    }
}