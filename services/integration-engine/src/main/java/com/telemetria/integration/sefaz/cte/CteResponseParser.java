package com.telemetria.integration.sefaz.cte;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public class CteResponseParser {

    public CteResultadoParse parseRetorno(String xmlRetorno) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xmlRetorno.getBytes(StandardCharsets.UTF_8))
        );

        String cStat = doc.getElementsByTagName("cStat").item(0).getTextContent();
        String xMotivo = doc.getElementsByTagName("xMotivo").item(0).getTextContent();
        
        String nProt = doc.getElementsByTagName("nProt").getLength() > 0 
                ? doc.getElementsByTagName("nProt").item(0).getTextContent() 
                : null;

        return new CteResultadoParse(cStat, xMotivo, nProt);
    }
}