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
import com.telemetria.integration.nfe.schemas.TRetConsReciNFe;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.wsdl.NFeRetAutorizacao.NFeRetAutorizacao4Stub;

import mockit.Mock;
import mockit.MockUp;

class ConsultaReciboTeste {

    private static final String RET_RECIBO_XML =
            "<retConsReciNFe versao=\"4.00\" xmlns=\"http://www.portalfiscal.inf.br/nfe\">" +
            "<tpAmb>2</tpAmb><cStat>104</cStat><xMotivo>Lote processado</xMotivo>" +
            "<cUF>35</cUF>" +
            "</retConsReciNFe>";

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
                    ConfiguracoesNfe cfg, String url) { }
        };
    }

    private void mockReciboStub() {
        new MockUp<NFeRetAutorizacao4Stub>() {
            @Mock
            public void $init(String endpoint) { }

            @Mock
            public NFeRetAutorizacao4Stub.NfeResultMsg nfeRetAutorizacaoLote(
                    NFeRetAutorizacao4Stub.NfeDadosMsg data) throws Exception {
                NFeRetAutorizacao4Stub.NfeResultMsg result = new NFeRetAutorizacao4Stub.NfeResultMsg();
                result.setExtraElement(AXIOMUtil.stringToOM(RET_RECIBO_XML));
                return result;
            }
        };
    }

    @Test
    void reciboNfe_reciboValido_retornaLoteProcessado() throws ExcecaoNfe {
        mockStubUtil();
        mockReciboStub();

        TRetConsReciNFe ret = ConsultaRecibo.reciboNfe(config, "135240000000001", DocumentoEnum.NFE);

        assertNotNull(ret);
        assertEquals("104", ret.getCStat());
    }

    @Test
    void reciboNfe_retornaMotivo() throws ExcecaoNfe {
        mockStubUtil();
        mockReciboStub();

        TRetConsReciNFe ret = ConsultaRecibo.reciboNfe(config, "135240000000001", DocumentoEnum.NFE);

        assertEquals("Lote processado", ret.getXMotivo());
    }

    @Test
    void reciboNfe_nfce_retornaResultado() throws ExcecaoNfe {
        mockStubUtil();
        mockReciboStub();

        TRetConsReciNFe ret = ConsultaRecibo.reciboNfe(config, "135240000000001", DocumentoEnum.NFCE);

        assertNotNull(ret);
        assertEquals("104", ret.getCStat());
    }
}
