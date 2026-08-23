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
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.util.CartaCorrecaoUtil;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.wsdl.NFeRecepcaoEvento.NFeRecepcaoEvento4Stub;

import mockit.Mock;
import mockit.MockUp;

class CartaCorrecaoTeste {

    private static final String RET_EVENTO_XML =
            "<retEnvEvento versao=\"1.00\" xmlns=\"http://www.portalfiscal.inf.br/nfe\">" +
            "<idLote>1</idLote><tpAmb>2</tpAmb><verAplic>TESTE</verAplic>" +
            "<cOrgao>91</cOrgao><cStat>128</cStat>" +
            "<xMotivo>Lote de Evento Processado</xMotivo>" +
            "</retEnvEvento>";

    private ConfiguracoesNfe config;
    private TEnvEventoCartaCorrecao enviEvento;

    @BeforeEach
    void setUp() throws ExcecaoNfe {
        config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.SP);
        config.setAmbiente(AmbienteEnum.HOMOLOGACAO);
        config.setEncode("UTF-8");

        config.setZoneId(java.time.ZoneId.of("America/Sao_Paulo"));

        Evento evento = new Evento();
        evento.setChave("52230309158456000159550010000731791567812345");
        evento.setCnpj("09158456000159");
        evento.setMotivo("Correcao no campo de endereco do destinatario da nota fiscal");
        evento.setSequencia(1);
        evento.setDataEvento(java.time.LocalDateTime.of(2024, 3, 10, 9, 0, 0));
        enviEvento = CartaCorrecaoUtil.montaCCe(evento, config);
    }

    private void mockStubUtil() {
        new MockUp<UtilitarioClienteAxis2>() {
            @Mock
            public void configuraHttpClient(org.apache.axis2.client.Stub stub,
                    ConfiguracoesNfe cfg, String url) { }
        };
    }

    private void mockAssinar() {
        new MockUp<Assinar>() {
            @Mock
            public String assinaNfe(ConfiguracoesNfe cfg, String xml,
                    AssinaturaEnum tipo) throws ExcecaoNfe {
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
    void eventoCCe_semValidacao_retornaEvento() throws ExcecaoNfe {
        mockStubUtil();
        mockAssinar();
        mockEventosStub();

        TRetEnvEventoCartaCorrecao ret = CartaCorrecao.eventoCCe(config, enviEvento, false);

        assertNotNull(ret);
        assertEquals("128", ret.getCStat());
    }

    @Test
    void eventoCCe_retornaLoteProcessado() throws ExcecaoNfe {
        mockStubUtil();
        mockAssinar();
        mockEventosStub();

        TRetEnvEventoCartaCorrecao ret = CartaCorrecao.eventoCCe(config, enviEvento, false);

        assertEquals("Lote de Evento Processado", ret.getXMotivo());
    }

    @Test
    void eventoCCe_retornaAmbienteHomologacao() throws ExcecaoNfe {
        mockStubUtil();
        mockAssinar();
        mockEventosStub();

        TRetEnvEventoCartaCorrecao ret = CartaCorrecao.eventoCCe(config, enviEvento, false);

        assertEquals("2", ret.getTpAmb());
    }
}
