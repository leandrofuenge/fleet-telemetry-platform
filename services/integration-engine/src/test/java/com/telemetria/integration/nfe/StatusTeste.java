package com.telemetria.integration.nfe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.axiom.om.util.AXIOMUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas.TRetConsStatServ;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.wsdl.NFeStatusServico4.NFeStatusServico4Stub;

import mockit.Mock;
import mockit.MockUp;

class StatusTeste {

    private static final String RET_STATUS_XML =
            "<retConsStatServ versao=\"4.00\" xmlns=\"http://www.portalfiscal.inf.br/nfe\">" +
            "<tpAmb>2</tpAmb><cStat>107</cStat><xMotivo>Servico em Operacao</xMotivo>" +
            "<cUF>35</cUF><dhRecbto>2024-01-01T10:00:00-03:00</dhRecbto><tMed>1</tMed>" +
            "</retConsStatServ>";

    private ConfiguracoesNfe config;

    @BeforeEach
    void setUp() {
        config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.SP);
        config.setAmbiente(AmbienteEnum.HOMOLOGACAO);
        config.setEncode("UTF-8");
    }

    private void mockStubUtil() {
        new MockUp<UtilitarioClienteAxis2>() {
            @Mock
            public void configuraHttpClient(org.apache.axis2.client.Stub stub,
                    ConfiguracoesNfe cfg, String url) { /* skip cert */ }
        };
    }

    private void mockStatusStub() {
        new MockUp<NFeStatusServico4Stub>() {
            @Mock
            public void $init(String endpoint) { /* skip Axis2 init */ }

            @Mock
            public NFeStatusServico4Stub.NfeResultMsg nfeStatusServicoNF(
                    NFeStatusServico4Stub.NfeDadosMsg data) throws Exception {
                NFeStatusServico4Stub.NfeResultMsg result = new NFeStatusServico4Stub.NfeResultMsg();
                result.setExtraElement(AXIOMUtil.stringToOM(RET_STATUS_XML));
                return result;
            }
        };
    }

    @Test
    void statusServico_nfe_retornaEstadoOperacional() throws ExcecaoNfe {
        mockStubUtil();
        mockStatusStub();

        TRetConsStatServ ret = Status.statusServico(config, DocumentoEnum.NFE);

        assertNotNull(ret);
        assertEquals("107", ret.getCStat());
        assertEquals("Servico em Operacao", ret.getXMotivo());
    }

    @Test
    void statusServico_nfce_retornaEstadoOperacional() throws ExcecaoNfe {
        mockStubUtil();
        mockStatusStub();

        TRetConsStatServ ret = Status.statusServico(config, DocumentoEnum.NFCE);

        assertNotNull(ret);
        assertEquals("107", ret.getCStat());
    }

    @Test
    void statusServico_retornaAmbienteHomologacao() throws ExcecaoNfe {
        mockStubUtil();
        mockStatusStub();

        TRetConsStatServ ret = Status.statusServico(config, DocumentoEnum.NFE);

        assertEquals("2", ret.getTpAmb());
    }
}
