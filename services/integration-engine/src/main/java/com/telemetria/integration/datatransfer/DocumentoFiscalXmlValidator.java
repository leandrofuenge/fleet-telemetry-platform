package com.telemetria.integration.datatransfer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.validation.CteXmlValidator;

@Component
public class DocumentoFiscalXmlValidator {
    private static final Map<String, Set<String>> RAIZES_PERMITIDAS = Map.of(
            "CTE", Set.of("CTe", "enviCTe"),
            "MDFE", Set.of("MDFe", "enviMDFe"),
            "NFE", Set.of("NFe", "enviNFe"));

    private final int maxDocumentBytes;
    private final CteXmlValidator cteXmlValidator;

    public DocumentoFiscalXmlValidator(
            @Value("${integration.data-transfer.max-document-bytes:1048576}") int maxDocumentBytes,
            CteXmlValidator cteXmlValidator) {
        this.maxDocumentBytes = maxDocumentBytes;
        this.cteXmlValidator = cteXmlValidator;
    }

    public void validar(String xml, String tipoDocumento) {
        if (xml == null || xml.isBlank()) {
            throw new DataTransferValidationException("O XML do documento não pode ser vazio.");
        }

        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxDocumentBytes) {
            throw new DataTransferValidationException(
                    "O XML excede o limite de " + maxDocumentBytes + " bytes.");
        }

        String tipo = normalizarTipo(tipoDocumento);
        Set<String> raizesPermitidas = RAIZES_PERMITIDAS.get(tipo);
        if (raizesPermitidas == null) {
            throw new DataTransferValidationException("Tipo de documento não suportado: " + tipoDocumento + ".");
        }

        try {
            Document document = criarFactorySegura().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(bytes));
            Element raiz = document.getDocumentElement();
            String nomeRaiz = raiz.getLocalName() == null ? raiz.getNodeName() : raiz.getLocalName();
            if (!raizesPermitidas.contains(nomeRaiz)) {
                throw new DataTransferValidationException(
                        "XML incompatível com " + tipo + ": esperada uma das raízes " + raizesPermitidas
                                + ", encontrada <" + nomeRaiz + ">.");
            }
            validarXsdQuandoDisponivel(xml, tipo, nomeRaiz);
        } catch (DataTransferValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DataTransferValidationException("XML inválido ou malformado.", exception);
        }
    }

    public int getMaxDocumentBytes() {
        return maxDocumentBytes;
    }

    private void validarXsdQuandoDisponivel(String xml, String tipoDocumento, String raiz) {
        if (!"CTE".equals(tipoDocumento) || !"CTe".equals(raiz)) {
            return;
        }
        try {
            cteXmlValidator.validarCte(xml);
        } catch (CteException exception) {
            throw new DataTransferValidationException(
                    "XML CT-e inválido conforme o XSD oficial 4.00: " + exception.getMessage(), exception);
        }
    }

    private DocumentBuilderFactory criarFactorySegura() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private String normalizarTipo(String tipoDocumento) {
        return tipoDocumento == null ? "CTE" : tipoDocumento.trim().toUpperCase();
    }
}
