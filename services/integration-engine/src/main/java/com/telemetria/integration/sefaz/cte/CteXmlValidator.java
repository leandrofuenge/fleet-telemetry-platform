package com.telemetria.integration.sefaz.cte;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Valida as mensagens XML do CT-e 4.00 contra os Schemas XSD oficiais embarcados.
 */
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
            throw new CteException("XML " + tipo.descricao + " não pode ser vazio ou nulo.");
        }

        Schema schema = schemas.get(tipo);
        if (schema == null) {
            throw new CteException("Schema XSD para " + tipo.descricao + " não foi carregado.");
        }

        try {
            Validator validator = schema.newValidator();
            // Proteção estrita contra ataques XXE (XML External Entity)
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            
            validator.validate(new StreamSource(new StringReader(xml)));

        } catch (SAXParseException e) {
            // Captura posição exata da falha no XML para facilitar o debug
            String detalheErro = String.format("XML %s inválido [Linha %d, Coluna %d]: %s",
                    tipo.descricao, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            throw new CteException(detalheErro, e);

        } catch (SAXException e) {
            throw new CteException("XML " + tipo.descricao + " incompatível com o XSD oficial CT-e 4.00: " + e.getMessage(), e);

        } catch (Exception e) {
            throw new CteException("Falha ao validar o XML " + tipo.descricao + " com o XSD oficial: " + e.getMessage(), e);
        }
    }

    private Schema carregarSchema(TipoDocumento tipo) {
        try {
            ClassPathResource resource = new ClassPathResource(SCHEMA_BASE + tipo.arquivo);
            URL schemaUrl = resource.getURL();

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            
            // Permite que o SchemaFactory resolva arquivos XSD inclusos (<xs:include>) dentro de JARs
            factory.setResourceResolver(new ClasspathResourceResolver(SCHEMA_BASE));
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar,all");

            return factory.newSchema(schemaUrl);

        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível carregar o XSD oficial " + tipo.arquivo + " no diretório " + SCHEMA_BASE, e);
        }
    }

    // =========================================================================
    // RESOLVER CUSTOMIZADO PARA INCLUDES DE XSD EM JAR EXECUTÁVEL
    // =========================================================================

    /**
     * Garante a resolução de dependências secundárias (ex: cteTiposBasico_v4.00.xsd, tiposGeral_v4.00.xsd)
     * quando a aplicação for empacotada em arquivo .jar.
     */
    private static class ClasspathResourceResolver implements LSResourceResolver {
        private final String basePath;

        public ClasspathResourceResolver(String basePath) {
            this.basePath = basePath;
        }

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            if (systemId == null) {
                return null;
            }

            String resourcePath = basePath + systemId;
            ClassPathResource resource = new ClassPathResource(resourcePath);

            if (!resource.exists()) {
                return null;
            }

            try {
                InputStream is = resource.getInputStream();
                return new SimpleLSInput(publicId, systemId, is);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static class SimpleLSInput implements LSInput {
        private final String publicId;
        private final String systemId;
        private final InputStream inputStream;

        public SimpleLSInput(String publicId, String systemId, InputStream inputStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.inputStream = inputStream;
        }

        @Override public InputStream getByteStream() { return inputStream; }
        @Override public void setByteStream(InputStream byteStream) {}
        @Override public java.io.Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(java.io.Reader characterStream) {}
        
        // CORREÇÃO AQUI: De StringData() para getStringData()
        @Override public String getStringData() { return null; }
        @Override public void setStringData(String stringData) {}
        
        @Override public String getEncoding() { return null; }
        @Override public void setEncoding(String encoding) {}
        @Override public String getPublicId() { return publicId; }
        @Override public void setPublicId(String publicId) {}
        @Override public String getSystemId() { return systemId; }
        @Override public void setSystemId(String systemId) {}
        @Override public String getBaseURI() { return null; }
        @Override public void setBaseURI(String baseURI) {}
        @Override public boolean getCertifiedText() { return false; }
        @Override public void setCertifiedText(boolean certifiedText) {}
    }

    // =========================================================================
    // TIPOS DE DOCUMENTO SUPORTADOS
    // =========================================================================

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