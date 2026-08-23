package com.telemetria.integration.sefaz.nfe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.schemas.TConsStatServ;

class NfeSchemaXmlSerializerTests {

    @Test
    void montaConsultaDeStatusComModeloGeradoDoSchemaOficial() {
        TConsStatServ consulta = new TConsStatServ();
        consulta.setVersao("4.00");
        consulta.setTpAmb("2");
        consulta.setCUF("51");
        consulta.setXServ("STATUS");

        String xml = new NfeSchemaXmlSerializer().serializar(consulta);

        assertThat(xml)
                .contains("<consStatServ")
                .contains("versao=\"4.00\"")
                .contains("<tpAmb>2</tpAmb>")
                .contains("<cUF>51</cUF>")
                .contains("<xServ>STATUS</xServ>");
    }
}
