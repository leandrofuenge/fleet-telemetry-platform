package com.telemetria.integration.sefaz.nfe.soap;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.telemetria.integration.sefaz.nfe.NfeSefazUnavailableException;

/** Verifica integridade do envelope SOAP e converte SOAP Fault em erro de infraestrutura. */
@Component
public class NfeSoapResponseValidator {

    public void validar(String respostaSoap, NfeSoapService service) {
        try {
            Document document = factorySegura().newDocumentBuilder().parse(
                    new ByteArrayInputStream(respostaSoap.getBytes(StandardCharsets.UTF_8)));
            NodeList faults = document.getElementsByTagNameNS("*", "Fault");
            if (faults.getLength() > 0) {
                Element fault = (Element) faults.item(0);
                throw new NfeSefazUnavailableException("SEFAZ retornou SOAP Fault para " + service.soapAction()
                        + ": " + texto(fault, "Text", texto(fault, "faultstring", "sem descrição")));
            }
            if (document.getElementsByTagNameNS("*", "Body").getLength() != 1) {
                throw new NfeSefazUnavailableException("Resposta SOAP NF-e sem Body válido.");
            }
        } catch (NfeSefazUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NfeSefazUnavailableException("Resposta SOAP inválida recebida da SEFAZ NF-e.", exception);
        }
    }

    private DocumentBuilderFactory factorySegura() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory;
    }

    private String texto(Element contexto, String nome, String padrao) {
        NodeList nodes = contexto.getElementsByTagNameNS("*", nome);
        return nodes.getLength() == 0 ? padrao : nodes.item(0).getTextContent().trim();
    }
}
