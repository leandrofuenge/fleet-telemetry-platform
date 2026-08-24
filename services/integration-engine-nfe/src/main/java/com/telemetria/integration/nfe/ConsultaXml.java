package com.telemetria.integration.nfe;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.kernel.http.HTTPConstants;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.EstadosEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas.TConsSitNFe;
import com.telemetria.integration.nfe.schemas.TRetConsSitNFe;
import com.telemetria.integration.nfe.util.ConstantesUtil;
import com.telemetria.integration.nfe.util.ObjetoUtil;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.util.UtilitarioServicoWeb;
import com.telemetria.integration.nfe.util.XmlNfeUtil;
import com.telemetria.integration.nfe.wsdl.NFeConsultaProtocolo.NFeConsultaProtocolo4Stub;

import br.com.swconsultoria.certificado.exception.CertificadoException;
import jakarta.xml.bind.JAXBException;

/**
 * Classe responsavel por Consultar a Situaçao do XML na SEFAZ.
 *
 */
class ConsultaXml {

    private static final Logger log = Logger.getLogger(ConsultaXml.class.getName());

    private ConsultaXml() {}

    /**
     * Classe Reponsavel Por Consultar o status da NFE na SEFAZ
     *
     * @param chave
     * @param tipoDocumento
     * @return
     * @throws ExcecaoNfe
     */
    static TRetConsSitNFe consultaXml(ConfiguracoesNfe config, String chave, DocumentoEnum tipoDocumento) throws ExcecaoNfe {

        try {

            TConsSitNFe consSitNFe = new TConsSitNFe();
            consSitNFe.setVersao(ConstantesUtil.VERSAO.NFE);
            consSitNFe.setTpAmb(config.getAmbiente().getCodigo());
            consSitNFe.setXServ("CONSULTAR");
            consSitNFe.setChNFe(chave);

            String xml = XmlNfeUtil.objectToXml(consSitNFe, config.getEncode());

            log.info("[XML-ENVIO]: " + xml);

            OMElement ome = AXIOMUtil.stringToOM(xml);

            String url = UtilitarioServicoWeb.getUrl(config, tipoDocumento, ServicosEnum.CONSULTA_XML);
            if (EstadosEnum.MS.equals(config.getEstado())) {
                com.telemetria.integration.nfe.wsdl.NFeConsultaProtocoloMS.NFeConsultaProtocolo4Stub.NfeDadosMsg dadosMsg = new com.telemetria.integration.nfe.wsdl.NFeConsultaProtocoloMS.NFeConsultaProtocolo4Stub.NfeDadosMsg();
                dadosMsg.setExtraElement(ome);

                com.telemetria.integration.nfe.wsdl.NFeConsultaProtocoloMS.NFeConsultaProtocolo4Stub stub =
                        new com.telemetria.integration.nfe.wsdl.NFeConsultaProtocoloMS.NFeConsultaProtocolo4Stub(url);

                UtilitarioClienteAxis2.configuraHttpClient(stub, config, url);

                // Timeout
                if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                            config.getTimeout());
                }
                com.telemetria.integration.nfe.wsdl.NFeConsultaProtocoloMS.NFeConsultaProtocolo4Stub.NfeResultMsg result = stub.nfeConsultaNF(dadosMsg);

                log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetConsSitNFe.class);
            } else {
                NFeConsultaProtocolo4Stub.NfeDadosMsg dadosMsg = new NFeConsultaProtocolo4Stub.NfeDadosMsg();
                dadosMsg.setExtraElement(ome);

                NFeConsultaProtocolo4Stub stub = new NFeConsultaProtocolo4Stub(
                        url);

                UtilitarioClienteAxis2.configuraHttpClient(stub, config, url);

                // Timeout
                if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                            config.getTimeout());
                }
                NFeConsultaProtocolo4Stub.NfeResultMsg result = stub.nfeConsultaNF(dadosMsg);

                log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetConsSitNFe.class);
            }

        } catch (RemoteException | XMLStreamException | JAXBException | CertificadoException e) {
            throw new ExcecaoNfe(e.getMessage(), e);
        }

    }

}

