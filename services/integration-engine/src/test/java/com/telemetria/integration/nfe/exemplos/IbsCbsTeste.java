package com.telemetria.integration.nfe.exemplos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas.TEnviNFe;
import com.telemetria.integration.nfe.schemas.TIBSCBSMonoTot;
import com.telemetria.integration.nfe.schemas.TNFe;
import com.telemetria.integration.nfe.schemas.TTribNFe;
import com.telemetria.integration.nfe.util.IbsCbsUtil;
import com.telemetria.integration.nfe.util.ObjetoUtil;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import jakarta.xml.bind.JAXBElement;

/**
 * @author Samuel Oliveira
 */
@EnabledIfEnvironmentVariable(named = "NFE_TEST_CERT_PATH", matches = ".+")
class IbsCbsTeste {

    @Test
    void testeIbsCbs() throws Exception {

        ConfiguracoesNfe config = getConfiguracoesNfe();
        TEnviNFe enviNFe = getEnviNFe();
        String json = getIbsCbsJson();
        String cclassTrib = "000001";

        IbsCbsUtil ibsCbsUtil = new IbsCbsUtil(json, DocumentoEnum.NFE);

        for (TNFe.InfNFe.Det det : enviNFe.getNFe().get(0).getInfNFe().getDet()) {
            TTribNFe ibsCbs = ibsCbsUtil.montaImpostosDet(cclassTrib, det);
            JAXBElement<TTribNFe> ibsCbsElement = new JAXBElement<>(new QName("IBSCBS"), TTribNFe.class, ibsCbs);
            det.getImposto().getContent().add(ibsCbsElement);
        }

        enviNFe = addTotaisIbsCbs(ibsCbsUtil, enviNFe, config);

        String esperado = XmlNfeUtil.leXml("src/test/resources/IbsCbs.xml");
        assertEquals(esperado, XmlNfeUtil.objectToXml(enviNFe));
    }

    @Test
    void testeIbsCbsRegular() throws Exception {

        ConfiguracoesNfe config = getConfiguracoesNfe();
        TEnviNFe enviNFe = getEnviNFe();
        String json = getIbsCbsJson();
        String cclassTrib = "550001";
        String cclassTribRegular = "000001";

        IbsCbsUtil ibsCbsUtil = new IbsCbsUtil(json, DocumentoEnum.NFE);

        for (TNFe.InfNFe.Det det : enviNFe.getNFe().get(0).getInfNFe().getDet()) {
            TTribNFe ibsCbs = ibsCbsUtil.montaImpostosDet(cclassTrib, det, cclassTribRegular);
            JAXBElement<TTribNFe> ibsCbsElement = new JAXBElement<>(new QName("IBSCBS"), TTribNFe.class, ibsCbs);
            det.getImposto().getContent().add(ibsCbsElement);

        }

        enviNFe = addTotaisIbsCbs(ibsCbsUtil, enviNFe, config);

        String esperado = XmlNfeUtil.leXml("src/test/resources/IbsCbsRegular.xml");
        assertEquals(esperado, XmlNfeUtil.objectToXml(enviNFe));
    }

    @Test
    void testeIbsCbsMonofasico() throws Exception {

        ConfiguracoesNfe config = getConfiguracoesNfe();
        TEnviNFe enviNFe = getEnviNFe();
        String json = getIbsCbsJson();
        String cclassTrib = "620006";

        IbsCbsUtil ibsCbsUtil = new IbsCbsUtil(json, DocumentoEnum.NFE);

        for (TNFe.InfNFe.Det det : enviNFe.getNFe().get(0).getInfNFe().getDet()) {
            TTribNFe ibsCbs = ibsCbsUtil.montaImpostosDet(cclassTrib, det);
            JAXBElement<TTribNFe> ibsCbsElement = new JAXBElement<>(new QName("IBSCBS"), TTribNFe.class, ibsCbs);
            det.getImposto().getContent().add(ibsCbsElement);

        }

        enviNFe = addTotaisIbsCbs(ibsCbsUtil, enviNFe, config);

        String esperado = XmlNfeUtil.leXml("src/test/resources/IbsCbsMonofasico.xml");
        assertEquals(esperado, XmlNfeUtil.objectToXml(enviNFe));
    }

    @Test
    void testeIbsCbsDiferimento() throws Exception {

        ConfiguracoesNfe config = getConfiguracoesNfe();
        TEnviNFe enviNFe = getEnviNFe();
        String json = getIbsCbsJson();
        String cclassTrib = "515001";

        IbsCbsUtil ibsCbsUtil = new IbsCbsUtil(json, DocumentoEnum.NFE);

        for (TNFe.InfNFe.Det det : enviNFe.getNFe().get(0).getInfNFe().getDet()) {
            ibsCbsUtil.setpAliqDiferimento(BigDecimal.valueOf(100));
            TTribNFe ibsCbs = ibsCbsUtil.montaImpostosDet(cclassTrib, det);
            JAXBElement<TTribNFe> ibsCbsElement = new JAXBElement<>(new QName("IBSCBS"), TTribNFe.class, ibsCbs);
            det.getImposto().getContent().add(ibsCbsElement);
        }

        enviNFe = addTotaisIbsCbs(ibsCbsUtil, enviNFe, config);

        String esperado = XmlNfeUtil.leXml("src/test/resources/IbsCbsDiferimento.xml");
        assertEquals(esperado, XmlNfeUtil.objectToXml(enviNFe));
    }

    private static TEnviNFe addTotaisIbsCbs(IbsCbsUtil ibsCbsUtil, TEnviNFe enviNFe, ConfiguracoesNfe config) throws ExcecaoNfe {
        BigDecimal vNfTot = ibsCbsUtil.calculaVnfTot(enviNFe.getNFe().get(0).getInfNFe().getTotal().getICMSTot().getVNF());
        enviNFe.getNFe().get(0).getInfNFe().getTotal().setVNFTot(ObjetoUtil.getValor2Casas(vNfTot));

        TIBSCBSMonoTot totaisIbsCsb = ibsCbsUtil.preencheTotaisIbsCsb();
        enviNFe.getNFe().get(0).getInfNFe().getTotal().setIBSCBSTot(totaisIbsCsb);
        return Nfe.montaNfe(config, enviNFe, false);
    }

    private static String getIbsCbsJson() throws IOException {
        return XmlNfeUtil.leXml("src/test/resources/ibscbs.json");
    }

    private static TEnviNFe getEnviNFe() throws IOException {
        String xml = XmlNfeUtil.leXml("src/test/resources/TesteXml.xml");
        return XmlNfeUtil.xmlToObject(xml, TEnviNFe.class);
    }

    private static ConfiguracoesNfe getConfiguracoesNfe() throws Exception {
        String caminhoCertificado = System.getenv("NFE_TEST_CERT_PATH");
        String senhaCertificado = System.getenv("NFE_TEST_CERT_PASSWORD");
        if (senhaCertificado == null || senhaCertificado.isBlank()) {
            throw new IllegalStateException("Defina NFE_TEST_CERT_PASSWORD para executar os testes de IBS/CBS.");
        }
        Certificado certificado = CertificadoService.certificadoPfx(caminhoCertificado, senhaCertificado);
        return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.GO, AmbienteEnum.HOMOLOGACAO, certificado, null);
    }

}
