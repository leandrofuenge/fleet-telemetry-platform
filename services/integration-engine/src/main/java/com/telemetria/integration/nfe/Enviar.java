package com.telemetria.integration.nfe;

import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.om.util.StAXParserConfiguration;
import org.apache.axis2.transport.http.HTTPConstants;
import org.xml.sax.InputSource;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AssinaturaEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.NfeException;
import com.telemetria.integration.nfe.schemas.TEnviNFe;
import com.telemetria.integration.nfe.schemas.TRetEnviNFe;
import com.telemetria.integration.nfe.util.ObjetoUtil;
import com.telemetria.integration.nfe.util.StubUtil;
import com.telemetria.integration.nfe.util.WebServiceUtil;
import com.telemetria.integration.nfe.util.XmlNfeUtil;
import com.telemetria.integration.nfe.ws.RetryParameter;
import com.telemetria.integration.nfe.wsdl.NFeAutorizacao.NFeAutorizacao4Stub;

import br.com.swconsultoria.certificado.exception.CertificadoException;
import jakarta.xml.bind.JAXBException;

/**
 * Classe Responsavel por Enviar o XML.
 *
 * @author Samuel Oliveira - samuel@swconsultoria.com.br - www.swconsultoria.com.br
 */
class Enviar {

    private static final Logger log = Logger.getLogger(Enviar.class.getName());

    private Enviar() {
    }

    /**
     * Metodo para Montar a NFE
     *
     * @param enviNFe
     * @param valida
     * @return
     * @throws NfeException
     */
    static TEnviNFe montaNfe(ConfiguracoesNfe config, TEnviNFe enviNFe, boolean valida) throws NfeException {

        try {

            /**
             * Cria o xml
             */
            String xml = XmlNfeUtil.objectToXml(enviNFe, config.getEncode());

            /**
             * Assina o Xml
             */
            xml = Assinar.assinaNfe(config, xml, AssinaturaEnum.NFE);

            //Retira Quebra de Linha
            xml = xml.replaceAll(System.lineSeparator(), "");

            log.info("[XML-ASSINADO]: " + xml);

            /**
             * Valida o Xml caso sejá selecionado True
             */
            if (valida) {
                new Validar().validaXml(config, xml, ServicosEnum.ENVIO);
            }

            return XmlNfeUtil.xmlToObject(xml, TEnviNFe.class);

        } catch (Exception e) {
            throw new NfeException(e.getMessage(), e);
        }

    }

    /**
     * Metodo para Enviar a NFE.
     *
     * @param enviNFe
     * @param tipoDocumento
     * @return
     * @throws NfeException
     */
    static TRetEnviNFe enviaNfe(ConfiguracoesNfe config, TEnviNFe enviNFe, DocumentoEnum tipoDocumento) throws NfeException {

        try {

            String xml = XmlNfeUtil.objectToXml(enviNFe, config.getEncode());

            OMElement ome;
            if (tipoDocumento.equals(DocumentoEnum.NFE)) {
                ome = AXIOMUtil.stringToOM(xml);
            } else {
                OMFactory factory = OMAbstractFactory.getOMFactory();
                ome = factory.getMetaFactory().createOMBuilder(factory, StAXParserConfiguration.NON_COALESCING, new InputSource(new StringReader(xml))).getDocumentElement();
            }

            Iterator<?> children = ome.getChildrenWithLocalName("NFe");
            while (children.hasNext()) {
                OMElement omElementNFe = (OMElement) children.next();
                if ((omElementNFe != null) && ("NFe".equals(omElementNFe.getLocalName()))) {
                    omElementNFe.addAttribute("xmlns", "http://www.portalfiscal.inf.br/nfe", null);
                }
            }

            log.info("[XML-ENVIO]: " + xml);

            NFeAutorizacao4Stub.NfeDadosMsg dadosMsg = new NFeAutorizacao4Stub.NfeDadosMsg();
            dadosMsg.setExtraElement(ome);

            String url = WebServiceUtil.getUrl(config, tipoDocumento, ServicosEnum.ENVIO);
            NFeAutorizacao4Stub stub = new NFeAutorizacao4Stub(url);

            StubUtil.configuraHttpClient(stub, config, url);

            // Timeout
            if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, config.getTimeout());
            }

            //Erro 411 MG
            if (tipoDocumento.equals(DocumentoEnum.NFCE) && config.getEstado().equals(EstadosEnum.MG)) {
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
            }

            if (ObjetoUtil.verifica(config.getRetry()).isPresent()) {
                RetryParameter.populateRetry(stub, config.getRetry());
            }

            NFeAutorizacao4Stub.NfeResultMsg result = stub.nfeAutorizacaoLote(dadosMsg);
            log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
            return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetEnviNFe.class);

        } catch (RemoteException | XMLStreamException | JAXBException | CertificadoException e) {
            throw new NfeException(e.getMessage(), e);
        }

    }

}