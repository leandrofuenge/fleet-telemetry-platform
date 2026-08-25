package com.telemetria.integration.nfe;

import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.om.util.StAXParserConfiguration;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.client.ServiceClient;

import com.telemetria.integration.nfe.codigo.gerado.schemas.TEnviNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetEnviNFe;
import com.telemetria.integration.nfe.codigo.gerado.wsdl.NFeAutorizacao.NFeAutorizacao4Stub;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.AssinaturaEnum;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.EstadosEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.util.UtilitarioServicoWeb;
import com.telemetria.integration.nfe.util.XmlNfeUtil;
import com.telemetria.integration.nfe.ws.ParametroTentativa;

import br.com.swconsultoria.certificado.exception.CertificadoException;
import jakarta.xml.bind.JAXBException;

/**
 * Classe responsável pelo envio da NF-e para a SEFAZ.
 */
class Enviar {

    private static final Logger log =
            Logger.getLogger(Enviar.class.getName());

    private static final String NAMESPACE_NFE =
            "http://www.portalfiscal.inf.br/nfe";

    private Enviar() {
    }

    /**
     * Monta, assina e opcionalmente valida a NF-e.
     *
     * @param config configurações da NF-e
     * @param enviNFe NF-e a ser montada
     * @param valida indica se o XML deve ser validado
     * @return NF-e montada
     * @throws ExcecaoNfe caso ocorra algum erro
     */
    static TEnviNFe montaNfe(
            ConfiguracoesNfe config,
            TEnviNFe enviNFe,
            boolean valida)
            throws ExcecaoNfe {

        validarParametros(config, enviNFe);

        try {

            String xml = XmlNfeUtil.objectToXml(
                    enviNFe,
                    config.getEncode()
            );

            validarXml(xml);

            xml = Assinar.assinaNfe(
                    config,
                    xml,
                    AssinaturaEnum.NFE
            );

            validarXml(xml);

            xml = removerQuebrasLinha(xml);

            if (log.isLoggable(Level.FINE)) {
                log.fine("[XML-ASSINADO]: " + xml);
            }

            if (valida) {
                new Validar().validaXml(
                        config,
                        xml,
                        ServicosEnum.ENVIO
                );
            }

            return XmlNfeUtil.xmlToObject(
                    xml,
                    TEnviNFe.class
            );

        } catch (JAXBException
                | CertificadoException
                | RuntimeException e) {

            throw new ExcecaoNfe(
                    "Erro ao montar e assinar a NF-e.",
                    e
            );
        }
    }

    /**
     * Envia a NF-e para a SEFAZ.
     *
     * @param config configurações da NF-e
     * @param enviNFe NF-e a ser enviada
     * @param tipoDocumento tipo do documento
     * @return retorno da SEFAZ
     * @throws ExcecaoNfe caso ocorra algum erro
     */
    static TRetEnviNFe enviaNfe(
            ConfiguracoesNfe config,
            TEnviNFe enviNFe,
            DocumentoEnum tipoDocumento)
            throws ExcecaoNfe {

        validarParametros(
                config,
                enviNFe,
                tipoDocumento
        );

        try {

            String xml = XmlNfeUtil.objectToXml(
                    enviNFe,
                    config.getEncode()
            );

            validarXml(xml);

            OMElement ome =
                    criarOmElement(xml, tipoDocumento);

            configurarNamespaceNfe(ome);

            String url =
                    obterUrl(config, tipoDocumento);

            validarUrl(
                    url,
                    config.getEstado()
            );

            NFeAutorizacao4Stub stub =
                    criarStub(config, url);

            configurarTimeout(stub, config);

            configurarRegrasEspecificas(
                    stub,
                    config,
                    tipoDocumento
            );

            configurarRetry(stub, config);

            NFeAutorizacao4Stub.NfeDadosMsg dadosMsg =
                    new NFeAutorizacao4Stub.NfeDadosMsg();

            dadosMsg.setExtraElement(ome);

            logEnvio(
                    config,
                    tipoDocumento
            );

            NFeAutorizacao4Stub.NfeResultMsg result =
                    stub.nfeAutorizacaoLote(dadosMsg);

            String xmlRetorno =
                    obterXmlRetorno(result);

            validarXmlRetorno(
                    xmlRetorno,
                    config.getEstado()
            );

            logRetorno(
                    config,
                    xmlRetorno
            );

            return XmlNfeUtil.xmlToObject(
                    xmlRetorno,
                    TRetEnviNFe.class
            );

        } catch (RemoteException e) {

            throw new ExcecaoNfe(
                    "Erro de comunicação com a SEFAZ "
                            + "durante o envio da NF-e.",
                    e
            );

        } catch (XMLStreamException e) {

            throw new ExcecaoNfe(
                    "Erro ao processar o XML da NF-e.",
                    e
            );

        } catch (JAXBException e) {

            throw new ExcecaoNfe(
                    "Erro ao converter o retorno da SEFAZ.",
                    e
            );

        } catch (CertificadoException e) {

            throw new ExcecaoNfe(
                    "Erro relacionado ao certificado digital "
                            + "durante o envio da NF-e.",
                    e
            );
        }
    }

    /**
     * Valida os parâmetros obrigatórios.
     */
    private static void validarParametros(
            ConfiguracoesNfe config,
            TEnviNFe enviNFe)
            throws ExcecaoNfe {

        if (config == null) {
            throw new ExcecaoNfe(
                    "As configurações da NFe não podem ser nulas."
            );
        }

        if (enviNFe == null) {
            throw new ExcecaoNfe(
                    "A NF-e não pode ser nula."
            );
        }
    }

    /**
     * Valida os parâmetros obrigatórios do envio.
     */
    private static void validarParametros(
            ConfiguracoesNfe config,
            TEnviNFe enviNFe,
            DocumentoEnum tipoDocumento)
            throws ExcecaoNfe {

        validarParametros(config, enviNFe);

        if (tipoDocumento == null) {
            throw new ExcecaoNfe(
                    "O tipo de documento não foi informado."
            );
        }

        if (config.getEstado() == null) {
            throw new ExcecaoNfe(
                    "O estado da NF-e não foi informado."
            );
        }
    }

    /**
     * Valida o XML.
     */
    private static void validarXml(
            String xml)
            throws ExcecaoNfe {

        if (xml == null || xml.isBlank()) {
            throw new ExcecaoNfe(
                    "O XML da NF-e não foi gerado corretamente."
            );
        }
    }

    /**
     * Remove quebras de linha do XML.
     */
    private static String removerQuebrasLinha(
            String xml) {

        if (xml == null) {
            return null;
        }

        return xml
                .replace("\r", "")
                .replace("\n", "");
    }

    /**
     * Cria o OMElement utilizado pelo Axis2.
     */
    private static OMElement criarOmElement(
            String xml,
            DocumentoEnum tipoDocumento)
            throws XMLStreamException {

        if (DocumentoEnum.NFE.equals(tipoDocumento)) {

            return AXIOMUtil.stringToOM(xml);
        }

        return OMXMLBuilderFactory
                .createOMBuilder(
                        StAXParserConfiguration.NON_COALESCING,
                        new StringReader(xml)
                )
                .getDocumentElement();
    }

    /**
     * Configura o namespace da NF-e.
     */
    private static void configurarNamespaceNfe(
            OMElement ome)
            throws ExcecaoNfe {

        if (ome == null) {
            throw new ExcecaoNfe(
                    "Não foi possível criar o XML para envio."
            );
        }

        Iterator<?> children =
                ome.getChildrenWithLocalName("NFe");

        while (children.hasNext()) {

            Object child = children.next();

            if (!(child instanceof OMElement)) {
                continue;
            }

            OMElement nfe =
                    (OMElement) child;

            if ("NFe".equals(nfe.getLocalName())) {

                nfe.addAttribute(
                        "xmlns",
                        NAMESPACE_NFE,
                        null
                );
            }
        }
    }

    /**
     * Obtém a URL do WebService.
     */
    private static String obterUrl(
            ConfiguracoesNfe config,
            DocumentoEnum tipoDocumento)
            throws ExcecaoNfe {

        String url =
                UtilitarioServicoWeb.getUrl(
                        config,
                        tipoDocumento,
                        ServicosEnum.ENVIO
                );

        if (url == null || url.isBlank()) {
            throw new ExcecaoNfe(
                    "URL do WebService de envio não encontrada "
                            + "para o estado "
                            + config.getEstado()
                            + "."
            );
        }

        return url.trim();
    }

    /**
     * Valida a URL do WebService.
     */
    private static void validarUrl(
            String url,
            EstadosEnum estado)
            throws ExcecaoNfe {

        if (url == null || url.isBlank()) {
            throw new ExcecaoNfe(
                    "URL do WebService de envio não encontrada "
                            + "para o estado "
                            + estado
                            + "."
            );
        }

        try {

            java.net.URI uri =
                    java.net.URI.create(url.trim());

            String scheme =
                    uri.getScheme();

            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme)
                    && !"https".equalsIgnoreCase(scheme))) {

                throw new ExcecaoNfe(
                        "URL do WebService inválida para o estado "
                                + estado
                                + ". O protocolo deve ser HTTP ou HTTPS."
                );
            }

            if (uri.getHost() == null
                    || uri.getHost().isBlank()) {

                throw new ExcecaoNfe(
                        "URL do WebService inválida para o estado "
                                + estado
                                + ". Host não informado."
                );
            }

        } catch (IllegalArgumentException e) {

            throw new ExcecaoNfe(
                    "URL do WebService inválida para o estado "
                            + estado
                            + ".",
                    e
            );
        }
    }

    /**
     * Cria e configura o Stub do Axis2.
     */
    private static NFeAutorizacao4Stub criarStub(
            ConfiguracoesNfe config,
            String url)
            throws CertificadoException {

        NFeAutorizacao4Stub stub =
                new NFeAutorizacao4Stub(url);

        UtilitarioClienteAxis2.configuraHttpClient(
                stub,
                config,
                url
        );

        return stub;
    }

    /**
     * Configura timeout de conexão e leitura.
     */
    private static void configurarTimeout(
            NFeAutorizacao4Stub stub,
            ConfiguracoesNfe config) {

        if (stub == null || config == null) {
            return;
        }

        if (ObjetoUtil.verifica(
                config.getTimeout()).isPresent()) {

            ServiceClient serviceClient =
                    stub._getServiceClient();

            serviceClient
                    .getOptions()
                    .setProperty(
                            HTTPConstants.SO_TIMEOUT,
                            config.getTimeout()
                    );

            serviceClient
                    .getOptions()
                    .setProperty(
                            HTTPConstants.CONNECTION_TIMEOUT,
                            config.getTimeout()
                    );
        }
    }

    /**
     * Configura regras específicas do WebService.
     */
    private static void configurarRegrasEspecificas(
            NFeAutorizacao4Stub stub,
            ConfiguracoesNfe config,
            DocumentoEnum tipoDocumento) {

        /*
         * Regra específica para NFC-e em Minas Gerais.
         *
         * Evita envio utilizando chunked encoding,
         * necessário devido ao comportamento do WebService.
         */
        if (DocumentoEnum.NFCE.equals(tipoDocumento)
                && EstadosEnum.MG.equals(
                        config.getEstado())) {

            stub
                    ._getServiceClient()
                    .getOptions()
                    .setProperty(
                            HTTPConstants.CHUNKED,
                            false
                    );
        }
    }

    /**
     * Configura mecanismo de retry.
     */
    private static void configurarRetry(
            NFeAutorizacao4Stub stub,
            ConfiguracoesNfe config) {

        if (ObjetoUtil.verifica(
                config.getRetry()).isPresent()) {

            ParametroTentativa.populateRetry(
                    stub,
                    config.getRetry()
            );
        }
    }

    /**
     * Obtém o XML retornado pela SEFAZ.
     */
    private static String obterXmlRetorno(
            NFeAutorizacao4Stub.NfeResultMsg result)
            throws ExcecaoNfe {

        if (result == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ não retornou uma resposta."
            );
        }

        if (result.getExtraElement() == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ retornou uma resposta sem "
                            + "conteúdo XML."
            );
        }

        String xmlRetorno =
                result.getExtraElement().toString();

        if (xmlRetorno == null
                || xmlRetorno.isBlank()) {

            throw new ExcecaoNfe(
                    "A SEFAZ retornou um XML vazio."
            );
        }

        return xmlRetorno;
    }

    /**
     * Valida o XML retornado pela SEFAZ.
     */
    private static void validarXmlRetorno(
            String xmlRetorno,
            EstadosEnum estado)
            throws ExcecaoNfe {

        if (xmlRetorno == null
                || xmlRetorno.isBlank()) {

            throw new ExcecaoNfe(
                    "A SEFAZ retornou um XML vazio "
                            + "para o estado "
                            + estado
                            + "."
            );
        }

        try {

            OMElement elemento =
                    AXIOMUtil.stringToOM(
                            xmlRetorno.trim()
                    );

            if (elemento == null) {
                throw new ExcecaoNfe(
                        "A SEFAZ retornou um XML inválido "
                                + "para o estado "
                                + estado
                                + "."
                );
            }

        } catch (XMLStreamException e) {

            throw new ExcecaoNfe(
                    "A SEFAZ retornou um XML malformado "
                            + "para o estado "
                            + estado
                            + ".",
                    e
            );
        }
    }

    /**
     * Registra o envio sem expor informações
     * desnecessárias no log.
     */
    private static void logEnvio(
            ConfiguracoesNfe config,
            DocumentoEnum tipoDocumento) {

        log.info(
                "[NFE-ENVIO] Documento="
                        + tipoDocumento
                        + ", Estado="
                        + config.getEstado()
        );
    }

    /**
     * Registra o retorno da SEFAZ somente em nível FINE.
     */
    private static void logRetorno(
            ConfiguracoesNfe config,
            String xmlRetorno) {

        if (log.isLoggable(Level.FINE)) {

            log.fine(
                    "[XML-RETORNO] Estado="
                            + config.getEstado()
                            + ": "
                            + xmlRetorno
            );
        }
    }
}