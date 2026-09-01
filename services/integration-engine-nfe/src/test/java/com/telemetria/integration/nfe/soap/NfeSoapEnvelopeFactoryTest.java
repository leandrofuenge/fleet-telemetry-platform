package com.telemetria.integration.nfe.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NfeSoapEnvelopeFactoryTest {

    private final NfeSoapEnvelopeFactory factory = new NfeSoapEnvelopeFactory();

    @Test
    void deveMontarEnvelopePadraoSemDuplicarDeclaracaoXml() {
        String envelope = factory.criar(
                NfeSoapService.STATUS,
                "\uFEFF <?xml version=\"1.0\" encoding=\"UTF-8\"?><consStatServ/>");

        assertThat(envelope)
                .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .contains("<nfeDadosMsg xmlns=\"" + NfeSoapService.STATUS.namespace() + "\">")
                .contains("<consStatServ/>")
                .doesNotContain("encoding=\"UTF-8\"?><consStatServ", "<nfeStatusServicoNF");
    }

    @Test
    void deveEncapsularDistribuicaoDfePeloMetodoWsdl() {
        String envelope = factory.criar(
                NfeSoapService.DISTRIBUICAO_DFE,
                "<distDFeInt/>");

        assertThat(envelope).contains(
                "<nfeDistDFeInteresse xmlns=\"" + NfeSoapService.DISTRIBUICAO_DFE.namespace() + "\">"
                        + "<nfeDadosMsg><distDFeInt/></nfeDadosMsg>"
                        + "</nfeDistDFeInteresse>");
    }

    @Test
    void deveRejeitarParametrosInvalidos() {
        assertThatThrownBy(() -> factory.criar(null, "<xml/>"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.criar(NfeSoapService.STATUS, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.criar(NfeSoapService.STATUS, "nao-e-xml"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
