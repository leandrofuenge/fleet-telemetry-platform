package com.telemetria.integration.nfe;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.transport.http.HTTPConstants;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AssinaturaEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.util.ObjetoUtil;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.util.UtilitarioServicoWeb;
import com.telemetria.integration.nfe.ws.ParametroTentativa;
import com.telemetria.integration.nfe.wsdl.NFeRecepcaoEvento.NFeRecepcaoEvento4Stub;

import br.com.swconsultoria.certificado.exception.CertificadoException;

class Eventos {

    private static final Logger log = Logger.getLogger(Eventos.class.getName());

    private Eventos() {
    }

    static String enviarEvento(ConfiguracoesNfe config, String xml, ServicosEnum tipoEvento, boolean valida, boolean assina, DocumentoEnum tipoDocumento)
            throws ExcecaoNfe {

        try {

            if (assina) {
                xml = Assinar.assinaNfe(config, xml, AssinaturaEnum.EVENTO);
            }

            log.info("[XML-ENVIO-" + tipoEvento + "]: " + xml);

            if (valida) {
                new Validar().validaXml(config, xml, tipoEvento);
            }

            OMElement ome = AXIOMUtil.stringToOM(xml);

            NFeRecepcaoEvento4Stub.NfeDadosMsg dadosMsg = new NFeRecepcaoEvento4Stub.NfeDadosMsg();
            dadosMsg.setExtraElement(ome);

            String url = UtilitarioServicoWeb.getUrl(config, tipoDocumento, tipoEvento);

            NFeRecepcaoEvento4Stub stub = new NFeRecepcaoEvento4Stub(url);

            UtilitarioClienteAxis2.configuraHttpClient(stub, config, url);

            // Timeout
            if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, config.getTimeout());
            }

            if (ObjetoUtil.verifica(config.getRetry()).isPresent()) {
                ParametroTentativa.populateRetry(stub, config.getRetry());
            }

            NFeRecepcaoEvento4Stub.NfeResultMsg result = stub.nfeRecepcaoEvento(dadosMsg);

            log.info("[XML-RETORNO-" + tipoEvento + "]: " + result.getExtraElement().toString());
            return result.getExtraElement().toString();
        } catch (RemoteException | XMLStreamException | CertificadoException e) {
            throw new ExcecaoNfe(e.getMessage(),e);
        }

    }
}
