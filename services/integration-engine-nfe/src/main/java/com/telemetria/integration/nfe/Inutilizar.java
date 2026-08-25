package com.telemetria.integration.nfe;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.kernel.http.HTTPConstants;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.AssinaturaEnum;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.EstadosEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TInutNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetInutNFe;
import com.telemetria.integration.nfe.util.ObjetoUtil;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.util.UtilitarioServicoWeb;
import com.telemetria.integration.nfe.util.XmlNfeUtil;
import com.telemetria.integration.nfe.codigo.gerado.wsdl.NFeInutilizacao.NFeInutilizacao4Stub;

import br.com.swconsultoria.certificado.exception.CertificadoException;
import jakarta.xml.bind.JAXBException;

/**
 * Classe Responsavel por inutilizar uma Faixa de numeracao da Nfe.
 *
 */
class Inutilizar {

    private static final Logger log = Logger.getLogger(Inutilizar.class.getName());

    private Inutilizar() {
    }

    static TRetInutNFe inutiliza(ConfiguracoesNfe config, TInutNFe inutNFe, DocumentoEnum tipoDocumento, boolean validar)
            throws ExcecaoNfe {

        try {

            String xml = XmlNfeUtil.objectToXml(inutNFe, config.getEncode());
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = Assinar.assinaNfe(config, xml, AssinaturaEnum.INUTILIZACAO);

            log.info("[XML-ENVIO]: " + xml);

            if (validar) {
                new Validar().validaXml(config, xml, ServicosEnum.INUTILIZACAO);
            }

            OMElement ome = AXIOMUtil.stringToOM(xml);

            String url = UtilitarioServicoWeb.getUrl(config, tipoDocumento, ServicosEnum.INUTILIZACAO);
            if (EstadosEnum.CE.equals(config.getEstado()) ) {
                com.telemetria.integration.nfe.codigo.gerado.wsdl.NFeInutilizacao.ce.NFeInutilizacao4Stub.NfeDadosMsg dadosMsgCe =
                        new  com.telemetria.integration.nfe.codigo.gerado.wsdl.NFeInutilizacao.ce.NFeInutilizacao4Stub.NfeDadosMsg();
                dadosMsgCe.setExtraElement(ome);
                com.telemetria.integration.nfe.codigo.gerado.wsdl.NFeInutilizacao.ce.NFeInutilizacao4Stub stubCe = new com.telemetria.integration.nfe.codigo.gerado.wsdl.NFeInutilizacao.ce.NFeInutilizacao4Stub(
                        url);
                UtilitarioClienteAxis2.configuraHttpClient(stubCe, config, url);

                // Timeout
                if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                    stubCe._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                    stubCe._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, config.getTimeout());
                }
                com.telemetria.integration.nfe.codigo.gerado.wsdl.NFeInutilizacao.ce.NFeInutilizacao4Stub.NfeResultMsg resultCe = stubCe.nfeInutilizacaoNF(dadosMsgCe);

                log.info("[XML-RETORNO]: " + resultCe.getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(resultCe.getExtraElement().toString(), TRetInutNFe.class);
            } else{
                NFeInutilizacao4Stub.NfeDadosMsg dadosMsg = new NFeInutilizacao4Stub.NfeDadosMsg();
                dadosMsg.setExtraElement(ome);
                NFeInutilizacao4Stub stub = new NFeInutilizacao4Stub(
                        url);

                UtilitarioClienteAxis2.configuraHttpClient(stub, config, url);

                // Timeout
                if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, config.getTimeout());
                }
                NFeInutilizacao4Stub.NfeResultMsg result = stub.nfeInutilizacaoNF(dadosMsg);

                log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetInutNFe.class);
            }

        } catch (RemoteException | XMLStreamException | JAXBException | CertificadoException e) {
            throw new ExcecaoNfe(e.getMessage(),e);
        }

    }

}

