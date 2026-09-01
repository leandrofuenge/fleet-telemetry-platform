package com.telemetria.integration.nfe.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.codigo.gerado.schemas.TConsStatServ;

class NfeSchemaXmlSerializerTest {

    private final NfeSchemaXmlSerializer serializer = new NfeSchemaXmlSerializer();

    @Test
    void deveSerializarDocumentoDoSchema() {
        TConsStatServ documento = new TConsStatServ();
        documento.setVersao("4.00");
        documento.setTpAmb("2");
        documento.setCUF("51");
        documento.setXServ("STATUS");

        assertThat(serializer.serializar(documento))
                .contains("consStatServ")
                .contains("versao=\"4.00\"")
                .contains("<tpAmb>2</tpAmb>");
    }

    @Test
    void deveRejeitarDocumentoNulo() {
        assertThatThrownBy(() -> serializer.serializar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser nulo");
    }
}
