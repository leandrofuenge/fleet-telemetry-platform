package com.telemetria.integration.sefaz.nfe;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class NfeXmlPayloadValidatorTests {

    private final NfeXmlPayloadValidator validator = new NfeXmlPayloadValidator(512);

    @Test
    void aceitaXmlNfeVersaoQuatroComRaizEsperada() {
        String xml = "<distDFeInt xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\"/>";

        assertThatCode(() -> validator.validar(xml, Set.of("distDFeInt"), "distribuição DFe"))
                .doesNotThrowAnyException();
    }

    @Test
    void bloqueiaRaizDeOutraOperacao() {
        String xml = "<inutNFe versao=\"4.00\"/>";

        assertThatThrownBy(() -> validator.validar(xml, Set.of("distDFeInt"), "distribuição DFe"))
                .isInstanceOf(NfeException.class)
                .hasMessageContaining("raiz esperada");
    }

    @Test
    void bloqueiaDoctypeParaEvitarEntidadeExterna() {
        String xml = "<!DOCTYPE nfe [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><distDFeInt versao=\"4.00\">&xxe;</distDFeInt>";

        assertThatThrownBy(() -> validator.validar(xml, Set.of("distDFeInt"), "distribuição DFe"))
                .isInstanceOf(NfeException.class)
                .hasMessageContaining("malformado");
    }
}
