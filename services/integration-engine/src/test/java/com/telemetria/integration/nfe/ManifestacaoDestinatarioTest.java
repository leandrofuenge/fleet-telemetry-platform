package com.telemetria.integration.nfe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.axiom.om.util.AXIOMUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.AssinaturaEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.ManifestacaoEnum;
import com.telemetria.integration.nfe.exception.NfeException;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoManifestacao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoManifestacao;
import com.telemetria.integration.nfe.util.ManifestacaoUtil;
import com.telemetria.integration.nfe.util.StubUtil;
import com.telemetria.integration.nfe.wsdl.NFeRecepcaoEvento.NFeRecepcaoEvento4Stub;

import mockit.Mock;
import mockit.MockUp;

class ManifestacaoDestinatarioTest {

    private static final String RET_EVENTO_XML =
            "<retEnvEvento versao=\"1.00\" xmlns=\"http://www.portalfiscal.inf.br/nfe\">" +
            "<idLote>1</idLote><tpAmb>2</tpAmb><verAplic>TESTE</verAplic>" +
            "<cOrgao>91</cOrgao><cStat>128</cStat>" +
            "<xMotivo>Lote de Evento Processado</xMotivo>" +
            "</retEnvEvento>";

    private ConfiguracoesNfe config;
    private TEnvEventoManifestacao enviEvento;

    @BeforeEach
    void setUp() throws NfeException {
        config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.SP);
        config.setAmbiente(AmbienteEnum.HOMOLOGACAO);
        config.setEncode("UTF-8");

        config.setZoneId(java.time.ZoneId.of("America/Sao_Paulo"));

        Evento evento = new Evento();
        evento.setChave("52230309158456000159550010000731791567812345");
        evento.setCnpj("09158456000159");
        evento.setProtocolo("352230000123456");
        evento.setTipoManifestacao(ManifestacaoEnum.CONFIRMACAO_DA_OPERACAO);
        evento.setDataEvento(java.time.LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        enviEvento = ManifestacaoUtil.montaManifestacao(evento, config);
    }

    private void mockStubUtil() {
        new MockUp<StubUtil>() {
            @Mock
            public void configuraHttpClient(org.apache.axis2.client.Stub stub,
                    ConfiguracoesNfe cfg, String url) { }
        };
    }

    private void mockAssinar() {
        new MockUp<Assinar>() {
            @Mock
            public String assinaNfe(ConfiguracoesNfe cfg, String xml,
                    AssinaturaEnum tipo) throws NfeException {
                return xml;
            }
        };
    }

    private void mockEventosStub() {
        new MockUp<NFeRecepcaoEvento4Stub>() {
            @Mock
            public void $init(String endpoint) { }

            @Mock
            public NFeRecepcaoEvento4Stub.NfeResultMsg nfeRecepcaoEvento(
                    NFeRecepcaoEvento4Stub.NfeDadosMsg data) throws Exception {
                NFeRecepcaoEvento4Stub.NfeResultMsg result = new NFeRecepcaoEvento4Stub.NfeResultMsg();
                result.setExtraElement(AXIOMUtil.stringToOM(RET_EVENTO_XML));
                return result;
            }
        };
    }

    @Test
    void eventoManifestacao_semValidacao_retornaEvento() throws NfeException {
        mockStubUtil();
        mockAssinar();
        mockEventosStub();

        TRetEnvEventoManifestacao ret = ManifestacaoDestinatario.eventoManifestacao(config, enviEvento, false);

        assertNotNull(ret);
        assertEquals("128", ret.getCStat());
    }

    @Test
    void eventoManifestacao_retornaLoteProcessado() throws NfeException {
        mockStubUtil();
        mockAssinar();
        mockEventosStub();

        TRetEnvEventoManifestacao ret = ManifestacaoDestinatario.eventoManifestacao(config, enviEvento, false);

        assertEquals("Lote de Evento Processado", ret.getXMotivo());
    }

    @Test
    void eventoManifestacao_retornaAmbienteHomologacao() throws NfeException {
        mockStubUtil();
        mockAssinar();
        mockEventosStub();

        TRetEnvEventoManifestacao ret = ManifestacaoDestinatario.eventoManifestacao(config, enviEvento, false);

        assertEquals("2", ret.getTpAmb());
    }
}
