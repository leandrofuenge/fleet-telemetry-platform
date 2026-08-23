package com.telemetria.integration.sefaz.nfe;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

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

    private static final Pattern CHAVE_ACESSO_PATTERN = Pattern.compile("\\d{44}");
    private static final Pattern N_REC_PATTERN = Pattern.compile("\\d{15}");

    private final NfeProperties properties;
    private final XmlSignatureValidator signatureValidator;
    private final NfeFiscalOperationGuard operationGuard;
    private final NfeXmlPayloadValidator xmlPayloadValidator;
    private final NfeSoapGateway soapGateway;

    public NfeClient(NfeProperties properties,
            XmlSignatureValidator signatureValidator, NfeFiscalOperationGuard operationGuard,
            NfeXmlPayloadValidator xmlPayloadValidator, NfeSoapGateway soapGateway) {
        this.properties = properties;
        this.signatureValidator = signatureValidator;
        this.operationGuard = operationGuard;
        this.xmlPayloadValidator = xmlPayloadValidator;
        this.soapGateway = soapGateway;
    }

    public String autorizarNfe(String xmlNfeAssinado) {
        operationGuard.exigirAutorizacaoPermitida();
        xmlPayloadValidator.validar(xmlNfeAssinado, Set.of("NFe", "enviNFe"), "autorização");
        validarXmlAssinado(xmlNfeAssinado, "infNFe");
        return enviar(NfeSoapService.AUTORIZACAO, properties.getEndpoints().getAutorizacao(), xmlNfeAssinado);
    }

    public String consultarReciboAutorizacao(String nRec) {
        exigirPadrao(nRec, N_REC_PATTERN, "nRec deve possuir 15 dígitos.");
        String xml = "<consReciNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><nRec>" + nRec + "</nRec></consReciNFe>";
        return enviar(NfeSoapService.RET_AUTORIZACAO, properties.getEndpoints().getRetAutorizacao(), xml);
    }

    public String consultarNfe(String chaveAcesso) {
        exigirPadrao(chaveAcesso, CHAVE_ACESSO_PATTERN, "A chave de acesso NF-e deve possuir 44 dígitos.");
        String xml = "<consSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><xServ>CONSULTAR</xServ><chNFe>"
                + chaveAcesso + "</chNFe></consSitNFe>";
        return enviar(NfeSoapService.CONSULTA, properties.getEndpoints().getConsulta(), xml);
    }

    public String consultarStatusServico() {
        String xml = "<consStatServ xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><cUF>" + properties.getCodigoUf()
                + "</cUF><xServ>STATUS</xServ></consStatServ>";
        return enviar(NfeSoapService.STATUS, properties.getEndpoints().getStatusServico(), xml);
    }

    public String enviarEvento(String xmlEventoAssinado) {
        operationGuard.exigirEventoPermitido();
        xmlPayloadValidator.validar(xmlEventoAssinado, Set.of("evento", "envEvento"), "evento");
        validarXmlAssinado(xmlEventoAssinado, "infEvento");
        return enviar(NfeSoapService.EVENTO, properties.getEndpoints().getEvento(), xmlEventoAssinado);
    }

    public String inutilizarNumeracao(String xmlInutAssinado) {
        operationGuard.exigirInutilizacaoPermitida();
        xmlPayloadValidator.validar(xmlInutAssinado, Set.of("inutNFe"), "inutilização");
        validarXmlAssinado(xmlInutAssinado, "infInut");
        return enviar(NfeSoapService.INUTILIZACAO, properties.getEndpoints().getInutilizacao(), xmlInutAssinado);
    }

    public String consultarDistribuicaoDfe(String xmlConsulta) {
        xmlPayloadValidator.validar(xmlConsulta, Set.of("distDFeInt"), "distribuição DFe");
        return enviar(NfeSoapService.DISTRIBUICAO_DFE, properties.getEndpoints().getDistribuicaoDfe(), xmlConsulta);
    }

    private void validarXmlAssinado(String xml, String element) {
        try {
            signatureValidator.validar(xml, element);
        } catch (RuntimeException exception) {
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
        return soapGateway.enviar(service, endpoint, xmlFiscal);
    }
}
