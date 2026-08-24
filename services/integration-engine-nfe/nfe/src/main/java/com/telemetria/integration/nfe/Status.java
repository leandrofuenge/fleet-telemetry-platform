package com.telemetria.integration.nfe;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas.TConsStatServ;
import com.telemetria.integration.nfe.schemas.TRetConsStatServ;
import com.telemetria.integration.nfe.util.ConstantesUtil;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.util.UtilitarioServicoWeb;
import com.telemetria.integration.nfe.util.XmlNfeUtil;
import com.telemetria.integration.nfe.wsdl.NFeStatusServico4.NFeStatusServico4Stub;

import br.com.swconsultoria.certificado.exception.CertificadoException;
import jakarta.xml.bind.JAXBException;

/**
 * Classe responsável por fazer a Verificação do Status Do Webservice
 *
 */
class Status {

    private static final Logger log = Logger.getLogger(Status.class.getName());

    /**
     * Metodo para Consulta de Status de Serviço
     * <p>
     * Cria um objeto do tipo TConsStatServ usando as propriedades passadas
     * pelo argumento <b>config</b>. Após, este objeto é convertido em um obejto
     * OMElement manipulável onde é passado para o atributo extraElement da
     * classe NFeStatusServico4Stub.NfeDadosMsg.
     * </p>
     *
     * <p>
     * O método statusServico então cria uma instância de NFeStatusServico4Stub
     * passando o argumento <b>tipo</b> e <b>config</b> em seu construtor, onde será montada a URL
     * de consulta do status do serviço dependendo das configuções
     * (ambiente, Estado, NF-e ou NFC-e)
     * </p>
     *
     * <p>
     * Então o método nfeStatusServicoNF efetuará a consulta e retornará o
     * resultado que será convertido em um objeto e enfim retornado por este
     * método.
     * </p>
     *
     * @param config        ConfiguracoesNfe, interface de configuração da NF-e ou NFC-e.
     * @param tipoDocumento ConstantesUtil.NFE ou ConstantesUtil.NFCE
     * @return TRetConsStatServ - objeto que contém o resultado da transmissão do XML.
     * @throws ExcecaoNfe
     * @see ConfiguracoesNfe
     * @see ConstantesUtil
     * @see UtilitarioServicoWeb
     * @see XmlNfeUtil
     */
    static TRetConsStatServ statusServico(ConfiguracoesNfe config, DocumentoEnum tipoDocumento) throws ExcecaoNfe {

        try {

            TConsStatServ consStatServ = new TConsStatServ();
            consStatServ.setTpAmb(config.getAmbiente().getCodigo());
            consStatServ.setCUF(config.getEstado().getCodigoUF());
            consStatServ.setVersao(ConstantesUtil.VERSAO.NFE);
            consStatServ.setXServ("STATUS");
            String xml = XmlNfeUtil.objectToXml(consStatServ, config.getEncode());

            log.info("[XML-ENVIO]: " + xml);

            OMElement ome = AXIOMUtil.stringToOM(xml);

            String url = UtilitarioServicoWeb.getUrl(config, tipoDocumento, ServicosEnum.STATUS_SERVICO);

            if (EstadosEnum.MS.equals(config.getEstado())) {
                com.telemetria.integration.nfe.wsdl.NFeStatusServico4MS.NFeStatusServico4Stub.NfeDadosMsg dadosMsg =
                        new com.telemetria.integration.nfe.wsdl.NFeStatusServico4MS.NFeStatusServico4Stub.NfeDadosMsg();
                dadosMsg.setExtraElement(ome);

                com.telemetria.integration.nfe.wsdl.NFeStatusServico4MS.NFeStatusServico4Stub stub =
                        new com.telemetria.integration.nfe.wsdl.NFeStatusServico4MS.NFeStatusServico4Stub(url);
                UtilitarioClienteAxis2.configuraHttpClient(stub, config, url);

                com.telemetria.integration.nfe.wsdl.NFeStatusServico4MS.NFeStatusServico4Stub.NfeResultMsg result = stub.nfeStatusServicoNF(dadosMsg);

                log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetConsStatServ.class);
            } else {
                NFeStatusServico4Stub.NfeDadosMsg dadosMsg = new NFeStatusServico4Stub.NfeDadosMsg();
                dadosMsg.setExtraElement(ome);

                NFeStatusServico4Stub stub = new NFeStatusServico4Stub(url);
                UtilitarioClienteAxis2.configuraHttpClient(stub, config, url);

                NFeStatusServico4Stub.NfeResultMsg result = stub.nfeStatusServicoNF(dadosMsg);

                log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetConsStatServ.class);
            }

        } catch (RemoteException | XMLStreamException | JAXBException | CertificadoException e) {
            throw new ExcecaoNfe(e.getMessage(),e);
        }
    }

}
