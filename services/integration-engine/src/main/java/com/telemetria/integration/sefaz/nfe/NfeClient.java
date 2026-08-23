package com.telemetria.integration.sefaz.nfe;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telemetria.integration.nfe.schemas.TConsReciNFe;
import com.telemetria.integration.nfe.schemas.TConsSitNFe;
import com.telemetria.integration.nfe.schemas.TConsStatServ;
import com.telemetria.integration.security.XmlSignatureValidator;
import com.telemetria.integration.sefaz.nfe.soap.NfeSoapGateway;
import com.telemetria.integration.sefaz.nfe.soap.NfeSoapService;

/**
 * Cliente SOAP 1.2 da NF-e 4.00, com mTLS e operações fiscais protegidas.
 *
 * <p>Usa {@link HttpClient} (Java 11+) em vez de {@code HttpsURLConnection}
 * para reaproveitar conexões, aplicar timeouts de forma explícita e evitar
 * vazamento de recursos.</p>
 */
@Component
public class NfeClient {

    private static final Logger log = LoggerFactory.getLogger(NfeClient.class);

    private static final Pattern CHAVE_ACESSO_PATTERN = Pattern.compile("\\d{44}");
    private static final Pattern N_REC_PATTERN = Pattern.compile("\\d{15}");

    private final NfeProperties properties;
    private final XmlSignatureValidator signatureValidator;
    private final NfeFiscalOperationGuard operationGuard;
    private final NfeXmlPayloadValidator xmlPayloadValidator;
    private final NfeSoapGateway soapGateway;
    private final NfeSchemaXmlSerializer schemaXmlSerializer;

    public NfeClient(NfeProperties properties,
            XmlSignatureValidator signatureValidator, NfeFiscalOperationGuard operationGuard,
            NfeXmlPayloadValidator xmlPayloadValidator, NfeSoapGateway soapGateway,
            NfeSchemaXmlSerializer schemaXmlSerializer) {
        this.properties = properties;
        this.signatureValidator = signatureValidator;
        this.operationGuard = operationGuard;
        this.xmlPayloadValidator = xmlPayloadValidator;
        this.soapGateway = soapGateway;
        this.schemaXmlSerializer = schemaXmlSerializer;
    }

    public String autorizarNfe(String xmlNfeAssinado) {
        log.info("NF-e: iniciando autorização no ambiente {}", properties.getAmbiente());
        operationGuard.exigirAutorizacaoPermitida();
        xmlPayloadValidator.validar(xmlNfeAssinado, Set.of("NFe", "enviNFe"), "autorização");
        validarXmlAssinado(xmlNfeAssinado, "infNFe");
        return enviar(NfeSoapService.AUTORIZACAO, properties.getEndpoints().getAutorizacao(), xmlNfeAssinado);
    }

    public String consultarReciboAutorizacao(String nRec) {
        log.info("NF-e: iniciando consulta de recibo no ambiente {}", properties.getAmbiente());
        exigirPadrao(nRec, N_REC_PATTERN, "nRec deve possuir 15 dígitos.");
        TConsReciNFe consulta = new TConsReciNFe();
        consulta.setVersao("4.00");
        consulta.setTpAmb(properties.getAmbiente());
        consulta.setNRec(nRec);
        String xml = schemaXmlSerializer.serializar(consulta);
        return enviar(NfeSoapService.RET_AUTORIZACAO, properties.getEndpoints().getRetAutorizacao(), xml);
    }

    public String consultarNfe(String chaveAcesso) {
        log.info("NF-e: iniciando consulta de documento no ambiente {}", properties.getAmbiente());
        exigirPadrao(chaveAcesso, CHAVE_ACESSO_PATTERN, "A chave de acesso NF-e deve possuir 44 dígitos.");
        TConsSitNFe consulta = new TConsSitNFe();
        consulta.setVersao("4.00");
        consulta.setTpAmb(properties.getAmbiente());
        consulta.setXServ("CONSULTAR");
        consulta.setChNFe(chaveAcesso);
        String xml = schemaXmlSerializer.serializar(consulta);
        return enviar(NfeSoapService.CONSULTA, properties.getEndpoints().getConsulta(), xml);
    }

    public String consultarStatusServico() {
        log.info("NF-e: iniciando consulta de status no ambiente {}", properties.getAmbiente());
        TConsStatServ consulta = new TConsStatServ();
        consulta.setVersao("4.00");
        consulta.setTpAmb(properties.getAmbiente());
        consulta.setCUF(properties.getCodigoUf());
        consulta.setXServ("STATUS");
        String xml = schemaXmlSerializer.serializar(consulta);
        return enviar(NfeSoapService.STATUS, properties.getEndpoints().getStatusServico(), xml);
    }

    public String enviarEvento(String xmlEventoAssinado) {
        log.info("NF-e: iniciando envio de evento no ambiente {}", properties.getAmbiente());
        operationGuard.exigirEventoPermitido();
        xmlPayloadValidator.validar(xmlEventoAssinado, Set.of("evento", "envEvento"), "evento");
        validarXmlAssinado(xmlEventoAssinado, "infEvento");
        return enviar(NfeSoapService.EVENTO, properties.getEndpoints().getEvento(), xmlEventoAssinado);
    }

    public String inutilizarNumeracao(String xmlInutAssinado) {
        log.info("NF-e: iniciando inutilização de numeração no ambiente {}", properties.getAmbiente());
        operationGuard.exigirInutilizacaoPermitida();
        xmlPayloadValidator.validar(xmlInutAssinado, Set.of("inutNFe"), "inutilização");
        validarXmlAssinado(xmlInutAssinado, "infInut");
        return enviar(NfeSoapService.INUTILIZACAO, properties.getEndpoints().getInutilizacao(), xmlInutAssinado);
    }

    public String consultarDistribuicaoDfe(String xmlConsulta) {
        log.info("NF-e: iniciando distribuição DFe no ambiente {}", properties.getAmbiente());
        xmlPayloadValidator.validar(xmlConsulta, Set.of("distDFeInt"), "distribuição DFe");
        return enviar(NfeSoapService.DISTRIBUICAO_DFE, properties.getEndpoints().getDistribuicaoDfe(), xmlConsulta);
    }

    private void validarXmlAssinado(String xml, String element) {
        try {
            signatureValidator.validar(xml, element);
            log.debug("NF-e: assinatura XML validada para o elemento {}", element);
        } catch (RuntimeException exception) {
            log.warn("NF-e: assinatura XML inválida para o elemento {}", element);
            throw new NfeException("Assinatura XMLDSig NF-e inválida: " + exception.getMessage(), exception);
        }
    }

    private void exigirXml(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML NF-e não pode ser vazio.");
        }
    }

    private void exigirPadrao(String valor, Pattern padrao, String mensagem) {
        if (valor == null || !padrao.matcher(valor).matches()) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private String enviar(NfeSoapService service, URI endpoint, String xmlFiscal) {
        log.info("NF-e: encaminhando operação SOAP {} (payloadBytes={})", service.name(),
                xmlFiscal.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        String resposta = soapGateway.enviar(service, endpoint, xmlFiscal);
        log.info("NF-e: operação SOAP {} concluída (respostaBytes={})", service.name(),
                resposta.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        return resposta;
    }
}
