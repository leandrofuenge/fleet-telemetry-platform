package com.telemetria.integration.sefaz.nfe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class NfeBase32CodecTests {

    @Test
    void codificaEDecodificaXmlComBase32Rfc4648() {
        NfeProperties properties = new NfeProperties();
        properties.setMaxXmlBytes(200);
        NfeBase32Codec codec = new NfeBase32Codec(properties);
        String xml = "<distDFeInt versao=\"4.00\">ação</distDFeInt>";

        NfeBase32Response resposta = codec.codificarResposta(xml);
        String decodificado = codec.decodificarXml(new NfeBase32Request(resposta.xmlBase32().toLowerCase()));

        assertThat(resposta.xmlBase32()).matches("[A-Z2-7]+$");
        assertThat(decodificado).isEqualTo(xml);
        assertThat(resposta.tamanhoXmlBytes()).isEqualTo(xml.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void rejeitaCaractereForaDoAlfabetoRfc4648() {
        NfeBase32Codec codec = new NfeBase32Codec(new NfeProperties());

        assertThatThrownBy(() -> codec.decodificarXml(new NfeBase32Request("ABC1")))
                .isInstanceOf(NfeException.class)
                .hasMessageContaining("Base32 RFC 4648 válido");
    }
}
