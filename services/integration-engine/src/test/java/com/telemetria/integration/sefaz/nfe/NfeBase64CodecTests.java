package com.telemetria.integration.sefaz.nfe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class NfeBase64CodecTests {

    @Test
    void decodificaEntradaECodificaRespostaEmUtf8() {
        NfeProperties properties = new NfeProperties();
        properties.setMaxXmlBytes(200);
        NfeBase64Codec codec = new NfeBase64Codec(properties);
        String xml = "<distDFeInt versao=\"4.00\">ação</distDFeInt>";

        String decodificado = codec.decodificarXml(new NfeBase64Request(
                Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8))));
        NfeBase64Response resposta = codec.codificarResposta(xml);

        assertThat(decodificado).isEqualTo(xml);
        assertThat(new String(Base64.getDecoder().decode(resposta.xmlBase64()), StandardCharsets.UTF_8)).isEqualTo(xml);
        assertThat(resposta.tamanhoXmlBytes()).isEqualTo(xml.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void rejeitaBase64InvalidoOuMaiorQueOLimite() {
        NfeProperties properties = new NfeProperties();
        properties.setMaxXmlBytes(3);
        NfeBase64Codec codec = new NfeBase64Codec(properties);

        assertThatThrownBy(() -> codec.decodificarXml(new NfeBase64Request("@@@")))
                .isInstanceOf(NfeException.class)
                .hasMessageContaining("Base64 válido");
        assertThatThrownBy(() -> codec.decodificarXml(new NfeBase64Request("PHhtbD4=")))
                .isInstanceOf(NfeException.class)
                .hasMessageContaining("excede o limite");
    }
}
