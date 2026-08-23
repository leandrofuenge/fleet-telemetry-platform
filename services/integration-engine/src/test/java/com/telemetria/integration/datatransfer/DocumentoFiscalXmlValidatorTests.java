package com.telemetria.integration.datatransfer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.sefaz.cte.validation.CteXmlValidator;

class DocumentoFiscalXmlValidatorTests {
    private final DocumentoFiscalXmlValidator validator = new DocumentoFiscalXmlValidator(1024, new CteXmlValidator());

    @Test
    void aceitaRaizDeMdfeCompativel() {
        assertDoesNotThrow(() -> validator.validar("<MDFe xmlns=\"http://www.portalfiscal.inf.br/mdfe\"/>", "MDFE"));
    }

    @Test
    void rejeitaRaizIncompativel() {
        assertThrows(DataTransferValidationException.class,
                () -> validator.validar("<NFe/>", "CTE"));
    }

    @Test
    void rejeitaXmlMalformado() {
        assertThrows(DataTransferValidationException.class,
                () -> validator.validar("<CTe>", "CTE"));
    }

    @Test
    void rejeitaCteForaDoSchemaOficial() {
        assertThrows(DataTransferValidationException.class,
                () -> validator.validar("<CTe xmlns=\"http://www.portalfiscal.inf.br/cte\"/>", "CTE"));
    }

    @Test
    void rejeitaDoctypeParaEvitarEntidadesExternas() {
        assertThrows(DataTransferValidationException.class,
                () -> validator.validar("<!DOCTYPE cte [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><CTe>&xxe;</CTe>", "CTE"));
    }
}
