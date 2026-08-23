package com.telemetria.integration.nfe;

import java.rmi.RemoteException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.transport.http.HTTPConstants;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.NfeException;
import com.telemetria.integration.nfe.schemas.TConsReciNFe;
import com.telemetria.integration.nfe.schemas.TRetConsReciNFe;
import com.telemetria.integration.nfe.util.ConstantesUtil;
import com.telemetria.integration.nfe.util.ObjetoUtil;
import com.telemetria.integration.nfe.util.StubUtil;
import com.telemetria.integration.nfe.util.WebServiceUtil;
import com.telemetria.integration.nfe.util.XmlNfeUtil;
import com.telemetria.integration.nfe.wsdl.NFeRetAutorizacao.NFeRetAutorizacao4Stub;

import br.com.swconsultoria.certificado.exception.CertificadoException;
import java.util.logging.Logger;

/**
 * Classe Responsavel Por pegar o Retorno da NFE, apos o Envio.
 *
 * @author Samuel Oliveira
 */
class ConsultaRecibo {

    private static final Logger log = Logger.getLogger(ConsultaRecibo.class.getName());

    private ConsultaRecibo() {
    }

    /**
     * Metodo Responsavel Por Pegar o Xml De Retorno.
     *
     * @param config        Configuracoes
     * @param recibo        Número Do Recibo para Consulta
     * @param tipoDocumento Informe {@link DocumentoEnum}
     * @return
     * @throws NfeException
     */
    static TRetConsReciNFe reciboNfe(ConfiguracoesNfe config, String recibo, DocumentoEnum tipoDocumento) throws NfeException {

        try {

            /**
             * Informaçoes do Certificado Digital.
             */

            TConsReciNFe consReciNFe = new TConsReciNFe();
            consReciNFe.setVersao(ConstantesUtil.VERSAO.NFE);
            consReciNFe.setTpAmb(config.getAmbiente().getCodigo());
            consReciNFe.setNRec(recibo);

            String xml = XmlNfeUtil.objectToXml(consReciNFe, config.getEncode());

            log.info("[XML-ENVIO]: " + xml);

            OMElement ome = AXIOMUtil.stringToOM(xml);
            NFeRetAutorizacao4Stub.NfeDadosMsg dadosMsg = new NFeRetAutorizacao4Stub.NfeDadosMsg();
            dadosMsg.setExtraElement(ome);

            String url = WebServiceUtil.getUrl(config, tipoDocumento, ServicosEnum.CONSULTA_RECIBO);
            NFeRetAutorizacao4Stub stub = new NFeRetAutorizacao4Stub(url);

            StubUtil.configuraHttpClient(stub, config, url);

            // Timeout
            if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                        config.getTimeout());
            }
            NFeRetAutorizacao4Stub.NfeResultMsg result = stub.nfeRetAutorizacaoLote(dadosMsg);

            log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
            return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetConsReciNFe.class);

        } catch (RemoteException | XMLStreamException | JAXBException | CertificadoException e) {
            throw new NfeException(e.getMessage(), e);
        }

    }
}
