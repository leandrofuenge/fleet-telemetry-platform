package com.telemetria.integration.sefaz.cte;

import java.io.StringReader;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/** Valida as mensagens CT-e 4.00 contra os XSD oficiais embarcados. */
@Component("cteXmlValidator")
public class CteXmlValidator {

    private static final String SCHEMA_BASE = "schemas/cte/4.00/";
    private final Map<TipoDocumento, Schema> schemas = new EnumMap<>(TipoDocumento.class);

    public CteXmlValidator() {
        for (TipoDocumento tipo : TipoDocumento.values()) {
            schemas.put(tipo, carregarSchema(tipo));
        }
    }

    public String validarEstrutura(String xml) {
        validarCte(xml);
        return xml;
    }

    public void validarCte(String xml) {
        validar(xml, TipoDocumento.CTE);
    }

    public void validarEvento(String xml) {
        validar(xml, TipoDocumento.EVENTO);
    }

    public void validarConsulta(String xml) {
        validar(xml, TipoDocumento.CONSULTA);
    }

    public void validarStatus(String xml) {
        validar(xml, TipoDocumento.STATUS);
    }

    private void validar(String xml, TipoDocumento tipo) {
        if (xml == null || xml.isBlank()) {
            throw new CteException("XML " + tipo.descricao + " não pode ser vazio.");
        }
        try {
            var validator = schemas.get(tipo).newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException e) {
            throw new CteException("XML " + tipo.descricao
                    + " inválido conforme o XSD oficial CT-e 4.00: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new CteException("Falha ao validar o XML " + tipo.descricao
                    + " com o XSD oficial CT-e 4.00.", e);
        }
    }

    private Schema carregarSchema(TipoDocumento tipo) {
        try {
            URL schemaUrl = new ClassPathResource(SCHEMA_BASE + tipo.arquivo).getURL();
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar");
            return factory.newSchema(schemaUrl);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível carregar o XSD oficial " + tipo.arquivo, e);
        }
    }

    private enum TipoDocumento {
        CTE("CT-e", "cte_v4.00.xsd"),
        EVENTO("de evento CT-e", "eventoCTe_v4.00.xsd"),
        CONSULTA("de consulta CT-e", "consSitCTe_v4.00.xsd"),
        STATUS("de status CT-e", "consStatServCTe_v4.00.xsd");

        private final String descricao;
        private final String arquivo;

        TipoDocumento(String descricao, String arquivo) {
            this.descricao = descricao;
            this.arquivo = arquivo;
        }
    }
}
