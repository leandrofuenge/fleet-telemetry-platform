package com.telemetria.integration.nfe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.axiom.om.util.AXIOMUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.AssinaturaEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas.TInutNFe;
import com.telemetria.integration.nfe.schemas.TRetInutNFe;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.wsdl.NFeInutilizacao.NFeInutilizacao4Stub;

import mockit.Mock;
import mockit.MockUp;

class InutilizarTeste {

    private static final String RET_INUT_XML =
            "<retInutNFe versao=\"4.00\" xmlns=\"http://www.portalfiscal.inf.br/nfe\">" +
            "<infInut Id=\"\">" +
            "<tpAmb>2</tpAmb><cStat>102</cStat>" +
            "<xMotivo>Inutilizacao de numero homologado</xMotivo>" +
            "<cUF>35</cUF><dhRecbto>2024-01-01T10:00:00-03:00</dhRecbto>" +
            "</infInut></retInutNFe>";

    private ConfiguracoesNfe config;
    private TInutNFe inutNFe;

    @BeforeEach
    void setUp() {
        config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.SP);
        config.setAmbiente(AmbienteEnum.HOMOLOGACAO);
        config.setEncode("UTF-8");

        inutNFe = new TInutNFe();
        TInutNFe.InfInut infInut = new TInutNFe.InfInut();
        infInut.setCNPJ("09158456000159");
        infInut.setAno("24");
        infInut.setMod("55");
        infInut.setSerie("001");
        infInut.setNNFIni("1");
        infInut.setNNFFin("1");
        infInut.setXJust("Inutilizacao de teste com justificativa longa");
        inutNFe.setInfInut(infInut);
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

    private void mockInutilizacaoStub() {
        new MockUp<NFeInutilizacao4Stub>() {
            @Mock
            public void $init(String endpoint) { }

            @Mock
            public NFeInutilizacao4Stub.NfeResultMsg nfeInutilizacaoNF(
                    NFeInutilizacao4Stub.NfeDadosMsg data) throws Exception {
                NFeInutilizacao4Stub.NfeResultMsg result = new NFeInutilizacao4Stub.NfeResultMsg();
                result.setExtraElement(AXIOMUtil.stringToOM(RET_INUT_XML));
                return result;
            }
        };
    }

    @Test
    void inutiliza_semValidacao_retornaInutilizacao() throws ExcecaoNfe {
        mockStubUtil();
        mockAssinar();
        mockInutilizacaoStub();

        TRetInutNFe ret = Inutilizar.inutiliza(config, inutNFe, DocumentoEnum.NFE, false);

        assertNotNull(ret);
        assertNotNull(ret.getInfInut());
    }

    @Test
    void inutiliza_retornaNumeroHomologado() throws ExcecaoNfe {
        mockStubUtil();
        mockAssinar();
        mockInutilizacaoStub();

        TRetInutNFe ret = Inutilizar.inutiliza(config, inutNFe, DocumentoEnum.NFE, false);

        assertEquals("102", ret.getInfInut().getCStat());
        assertEquals("Inutilizacao de numero homologado", ret.getInfInut().getXMotivo());
    }

    @Test
    void inutiliza_retornaAmbienteHomologacao() throws ExcecaoNfe {
        mockStubUtil();
        mockAssinar();
        mockInutilizacaoStub();

        TRetInutNFe ret = Inutilizar.inutiliza(config, inutNFe, DocumentoEnum.NFE, false);

        assertEquals("2", ret.getInfInut().getTpAmb());
    }
}
