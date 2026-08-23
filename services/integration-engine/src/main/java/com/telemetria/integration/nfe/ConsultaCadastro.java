package com.telemetria.integration.nfe;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.transport.http.HTTPConstants;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.PessoaEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.NfeException;
import com.telemetria.integration.nfe.schemas.TConsCad;
import com.telemetria.integration.nfe.schemas.TRetConsCad;
import com.telemetria.integration.nfe.schemas.TUfCons;
import com.telemetria.integration.nfe.util.ConstantesUtil;
import com.telemetria.integration.nfe.util.ObjetoUtil;
import com.telemetria.integration.nfe.util.StubUtil;
import com.telemetria.integration.nfe.util.WebServiceUtil;
import com.telemetria.integration.nfe.util.XmlNfeUtil;
import com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.CadConsultaCadastro4Stub;

import br.com.swconsultoria.certificado.exception.CertificadoException;
import jakarta.xml.bind.JAXBException;

/**
 * Classe responsavel por Consultar a Situaçao do XML na SEFAZ.
 *
 * @author Samuel Oliveira - samuel@swconsultoria.com.br - www.swconsultoria.com.br
 */
class ConsultaCadastro {

    private static final Logger log = Logger.getLogger(ConsultaCadastro.class.getName());

    private ConsultaCadastro() {}

    /**
     * Classe Reponsavel Por Consultar o status da NFE na SEFAZ
     */

    static TRetConsCad consultaCadastro(ConfiguracoesNfe config, PessoaEnum tipoPessoa, String cnpjCpf, EstadosEnum estado)
            throws NfeException {

        try {

            TConsCad consCad = new TConsCad();
            consCad.setVersao(ConstantesUtil.VERSAO.CONSULTA_CADASTRO);

            TConsCad.InfCons infCons = new TConsCad.InfCons();
            if (PessoaEnum.JURIDICA.equals(tipoPessoa)) {
                infCons.setCNPJ(cnpjCpf);
            } else {
                infCons.setCPF(cnpjCpf);
            }
            infCons.setXServ("CONS-CAD");
            infCons.setUF(TUfCons.valueOf(estado.toString()));

            consCad.setInfCons(infCons);

            String xml = XmlNfeUtil.objectToXml(consCad, config.getEncode());

            log.info("[XML-ENVIO]: " + xml);

            OMElement ome = AXIOMUtil.stringToOM(xml);

            ConfiguracoesNfe configConsulta = new ConfiguracoesNfe();
            configConsulta.setContigenciaSVC(config.isContigenciaSVC());
            configConsulta.setEstado(estado);
            configConsulta.setAmbiente(config.getAmbiente());

            String url = WebServiceUtil.getUrl(configConsulta, DocumentoEnum.NFE, ServicosEnum.CONSULTA_CADASTRO);
            if (EstadosEnum.MS.equals(estado)) {
                com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub.NfeDadosMsg dadosMsg =
                        new com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub.NfeDadosMsg();
                dadosMsg.setExtraElement(ome);

                com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub stub =
                        new com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub(url);

                StubUtil.configuraHttpClient(stub, config, url);

                // Timeout
                if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                            config.getTimeout());
                }

                com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub.NfeResultMsg result = stub.consultaCadastro(dadosMsg);

                log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetConsCad.class);
            } else if (EstadosEnum.MT.equals(estado)) {
                com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.ConsultaCadastro consultaCadastro =
                        new com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.ConsultaCadastro();
                com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.NfeDadosMsg_type0 dadosMsg = new com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.NfeDadosMsg_type0();
                dadosMsg.setExtraElement(ome);
                consultaCadastro.setNfeDadosMsg(dadosMsg);

                com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub stub =
                        new com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub(url);

                StubUtil.configuraHttpClient(stub, config, url);

                // Timeout
                if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                            config.getTimeout());
                }

                com.telemetria.integration.nfe.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.NfeResultMsg result = stub.consultaCadastro(consultaCadastro);

                log.info("[XML-RETORNO]: " + result.getConsultaCadastroResult().getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(result.getConsultaCadastroResult().getExtraElement().toString(), TRetConsCad.class);
            } else {
                CadConsultaCadastro4Stub.NfeDadosMsg dadosMsg = new CadConsultaCadastro4Stub.NfeDadosMsg();
                dadosMsg.setExtraElement(ome);

                CadConsultaCadastro4Stub stub = new CadConsultaCadastro4Stub(url);

                StubUtil.configuraHttpClient(stub, config, url);

                // Timeout
                if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                    stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                            config.getTimeout());
                }

                CadConsultaCadastro4Stub.NfeResultMsg result = stub.consultaCadastro(dadosMsg);

                log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
                return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetConsCad.class);
            }

        } catch (RemoteException | XMLStreamException | JAXBException | CertificadoException e) {
            throw new NfeException(e.getMessage(), e);
        }

    }

}