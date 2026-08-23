/**
 *
 */
package com.telemetria.integration.nfe.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import com.telemetria.integration.nfe.dom.enuns.XsdEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas.TEnviNFe;
import com.telemetria.integration.nfe.schemas.TNfeProc;
import com.telemetria.integration.nfe.schemas.TProtNFe;

import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

/**
 * Classe Responsavel por Metodos referentes ao XML
 *
 * @author Samuel Oliveira
 */
public class XmlNfeUtil {

    private static final Logger log = Logger.getLogger(XmlNfeUtil.class.getName());

    private XmlNfeUtil() {}

    /**
     * Transforma o String do XML em Objeto
     *
     * @param xml
     * @param classe
     * @return T
     */
    public static <T> T xmlToObject(String xml, Class<T> classe) {
        return JAXB.unmarshal(new StreamSource(new StringReader(xml)), classe);
    }

    /**
     * Transforma o Objeto em XML(String)
     *
     * @param obj
     * @return
     * @throws JAXBException
     * @throws ExcecaoNfe
     */
    public static String objectToXml(Object objeto) throws JAXBException, ExcecaoNfe {
        return objectToXml(objeto, null, null, StandardCharsets.UTF_8);
    }

    public static String objectToXml(Object objeto, Charset encode) throws JAXBException, ExcecaoNfe {
        return objectToXml(objeto, null, null, encode);
    }

    public static <T> String objectToXml(T objeto, Class<T> clazz, String nomeElemento) throws JAXBException, ExcecaoNfe {
        return objectToXml(objeto, clazz, nomeElemento, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static <T> String objectToXml(T objeto, Class<T> clazz, String nomeElemento, Charset encode) throws JAXBException, ExcecaoNfe {

        JAXBContext context = JAXBContext.newInstance(objeto.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty("jaxb.encoding", "Unicode");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        StringWriter sw = new StringWriter();

        String encodeXml = encode == null || !Charset.isSupported(encode.displayName()) ? "UTF-8" : encode.displayName();
        sw.append("<?xml version=\"1.0\" encoding=\"").append(encodeXml).append("\"?>");

        Result result = new StreamResult(sw);

        if (clazz == null) {
            XsdEnum xsd = XsdEnum.getByClassName(objeto.getClass().getName());
            clazz = (Class<T>) xsd.getClazz();
            nomeElemento = xsd.getName();
        }

        marshaller.marshal(new JAXBElement<>(
                new QName("http://www.portalfiscal.inf.br/nfe", nomeElemento),
                clazz, objeto), result);

        return replacesNfe(sw.toString());

    }

    public static String gZipToXml(byte[] conteudo) throws IOException {
        if (conteudo == null || conteudo.length == 0) {
            return "";
        }
        GZIPInputStream gis;
        gis = new GZIPInputStream(new ByteArrayInputStream(conteudo));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
        StringBuilder outStr = new StringBuilder();
        String line;
        while ((line = bf.readLine()) != null) {
            outStr.append(line);
        }

        return outStr.toString();
    }

    public static String criaNfeProc(TEnviNFe enviNfe, Object retorno) throws JAXBException, ExcecaoNfe {

        TNfeProc nfeProc = new TNfeProc();
        nfeProc.setVersao("4.00");
        nfeProc.setNFe(enviNfe.getNFe().get(0));
        String xml = XmlNfeUtil.objectToXml(retorno);
        nfeProc.setProtNFe(XmlNfeUtil.xmlToObject(xml, TProtNFe.class));

        return XmlNfeUtil.objectToXml(nfeProc);
    }

    private static String replacesNfe(String xml) {

        return xml.replace("<!\\[CDATA\\[<!\\[CDATA\\[", "<!\\[CDATA\\[")
                .replace("\\]\\]>\\]\\]>", "\\]\\]>")
                .replace("ns2:", "")
                .replace("ns3:", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("<Signature>", "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">")
                .replace(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "")
                .replace(" xmlns=\"\" xmlns:ns3=\"http://www.portalfiscal.inf.br/nfe\"", "")
                .replace("<NFe>", "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">");

    }

    /**
     * Le o Arquivo XML e retona String
     *
     * @return String
     * @throws ExcecaoNfe
     */
    public static String leXml(String arquivo) throws IOException {

        ObjetoUtil.verifica(arquivo).orElseThrow(() -> new IllegalArgumentException("Arquivo xml não pode ser nulo/vazio."));
        if (!Files.exists(Paths.get(arquivo))) {
            throw new FileNotFoundException("Arquivo " + arquivo + " não encontrado.");
        }
        List<String> list = Files.readAllLines(Paths.get(arquivo));
        StringJoiner joiner = new StringJoiner("\n");
        list.forEach(joiner::add);

        return joiner.toString();
    }

    public static String dataNfe(LocalDateTime dataASerFormatada) {
        return dataNfe(dataASerFormatada, ZoneId.systemDefault());
    }

    public static String dataNfe(LocalDateTime dataASerFormatada, ZoneId zoneId) {
        try {
            GregorianCalendar calendar = GregorianCalendar.from(dataASerFormatada.atZone(ObjetoUtil.verifica(zoneId).orElse(ZoneId.of("Brazil/East"))));

            XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
            xmlCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            return xmlCalendar.toString();

        } catch (DatatypeConfigurationException e) {
            log.warning(e.getMessage());
        }
        return null;
    }

    public static String getTag(String xml, String tag) throws ExcecaoNfe {
        if (xml == null || xml.isEmpty()) {
            throw new ExcecaoNfe("XML de entrada está vazio.");
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            XPath xPath = XPathFactory.newInstance().newXPath();
            Node node = (Node) xPath.evaluate("//*[local-name()='" + tag + "']", doc, XPathConstants.NODE);

            if (node == null) {
                throw new ExcecaoNfe("Tag '" + tag + "' não encontrada no XML.");
            }

            return nodeToString(node);

        } catch (Exception e) {
            throw new ExcecaoNfe("Erro ao extrair a tag '" + tag + "' do XML.\nErro: " + e.getMessage(), e);
        }
    }

    private static String nodeToString(Node node) {
        Document document = node.getOwnerDocument();
        DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation().getFeature("LS", "3.0");
        LSSerializer serializer = domImplLS.createLSSerializer();
        serializer.getDomConfig().setParameter("xml-declaration", false);
        return serializer.writeToString(node);
    }

    public static <T> Element objectToElement(Object objeto, Class<T> classe) throws ExcecaoNfe {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            JAXBContext context = JAXBContext.newInstance(classe);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(objeto, document);

            return document.getDocumentElement();

        } catch (Exception e) {
            throw new ExcecaoNfe("Erro Ao Converter Objeto em Elemento: ", e);
        }
    }
}
