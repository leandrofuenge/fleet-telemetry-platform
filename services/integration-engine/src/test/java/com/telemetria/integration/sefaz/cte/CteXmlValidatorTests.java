package com.telemetria.integration.sefaz.cte;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CteXmlValidatorTests {

    private final CteXmlValidator validator = new CteXmlValidator();

    @Test
    void deveValidarConsultaConformeSchemaOficial() {
        String xml = """
                <consSitCTe versao="4.00" xmlns="http://www.portalfiscal.inf.br/cte">
                  <tpAmb>2</tpAmb>
                  <xServ>CONSULTAR</xServ>
                  <chCTe>51260812345678000123570010000000011000000010</chCTe>
                </consSitCTe>
                """;
        assertDoesNotThrow(() -> validator.validarConsulta(xml));
    }

    @Test
    void deveValidarStatusConformeSchemaOficial() {
        String xml = """
                <consStatServCTe versao="4.00" xmlns="http://www.portalfiscal.inf.br/cte">
                  <tpAmb>2</tpAmb>
                  <cUF>51</cUF>
                  <xServ>STATUS</xServ>
                </consStatServCTe>
                """;
        assertDoesNotThrow(() -> validator.validarStatus(xml));
    }

    @Test
    void deveRejeitarConsultaForaDoLayoutOficial() {
        CteException exception = assertThrows(CteException.class,
                () -> validator.validarConsulta("""
                        <consSitCTe versao="4.00" xmlns="http://www.portalfiscal.inf.br/cte">
                          <tpAmb>9</tpAmb>
                          <xServ>INVALIDO</xServ>
                          <chCTe>123</chCTe>
                        </consSitCTe>
                        """));
        assertTrue(exception.getMessage().contains("XSD oficial CT-e 4.00"));
    }
}
