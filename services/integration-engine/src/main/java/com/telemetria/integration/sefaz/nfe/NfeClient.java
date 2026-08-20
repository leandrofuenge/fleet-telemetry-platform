package com.telemetria.integration.sefaz.nfe;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.telemetria.integration.security.XmlSignatureValidator;

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

    private static final String NS_AUTORIZACAO = "http://www.portalfiscal.inf.br/nfe/wsdl/NfeAutorizacao4";
    private static final String NS_RET_AUTORIZACAO = "http://www.portalfiscal.inf.br/nfe/wsdl/NfeRetAutorizacao4";
    private static final String NS_CONSULTA = "http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4";
    private static final String NS_STATUS_SERVICO = "http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico4";
    private static final String NS_EVENTO = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4";
    private static final String NS_INUTILIZACAO = "http://www.portalfiscal.inf.br/nfe/wsdl/NfeInutilizacao4";
    private static final String NS_DISTRIBUICAO_DFE = "http://www.portalfiscal.inf.br/nfe/wsdl/NFeDistribuicaoDFe";

    private final NfeProperties properties;
    private final SSLContext sslContext;
    private final XmlSignatureValidator signatureValidator;
    private final NfeFiscalOperationGuard operationGuard;
    private final HttpClient httpClient;

    public NfeClient(NfeProperties properties, @Qualifier("sefazSslContext") SSLContext sslContext,
            XmlSignatureValidator signatureValidator, NfeFiscalOperationGuard operationGuard) {
        this.properties = properties;
        this.sslContext = sslContext;
        this.signatureValidator = signatureValidator;
        this.operationGuard = operationGuard;
        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofMillis(properties.getTimeoutMillis()))
                .build();
    }

    public String autorizarNfe(String xmlNfeAssinado) {
        operationGuard.exigirAutorizacaoPermitida();
        validarXmlAssinado(xmlNfeAssinado, "infNFe");
        return enviar(properties.getEndpoints().getAutorizacao(), xmlNfeAssinado,
                NS_AUTORIZACAO, NS_AUTORIZACAO + "/nfeAutorizacaoLote");
    }

    public String consultarReciboAutorizacao(String nRec) {
        exigirPadrao(nRec, N_REC_PATTERN, "nRec deve possuir 15 dígitos.");
        String xml = "<consReciNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><nRec>" + nRec + "</nRec></consReciNFe>";
        return enviar(properties.getEndpoints().getRetAutorizacao(), xml,
                NS_RET_AUTORIZACAO, NS_RET_AUTORIZACAO + "/nfeRetAutorizacaoLote");
    }

    public String consultarNfe(String chaveAcesso) {
        exigirPadrao(chaveAcesso, CHAVE_ACESSO_PATTERN, "A chave de acesso NF-e deve possuir 44 dígitos.");
        String xml = "<consSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><xServ>CONSULTAR</xServ><chNFe>"
                + chaveAcesso + "</chNFe></consSitNFe>";
        return enviar(properties.getEndpoints().getConsulta(), xml,
                NS_CONSULTA, NS_CONSULTA + "/nfeConsultaNF");
    }

    public String consultarStatusServico() {
        String xml = "<consStatServ xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><cUF>" + properties.getCodigoUf()
                + "</cUF><xServ>STATUS</xServ></consStatServ>";
        return enviar(properties.getEndpoints().getStatusServico(), xml,
                NS_STATUS_SERVICO, NS_STATUS_SERVICO + "/nfeStatusServicoNF");
    }

    public String enviarEvento(String xmlEventoAssinado) {
        operationGuard.exigirEventoPermitido();
        validarXmlAssinado(xmlEventoAssinado, "infEvento");
        return enviar(properties.getEndpoints().getEvento(), xmlEventoAssinado,
                NS_EVENTO, NS_EVENTO + "/nfeRecepcaoEvento");
    }

    public String inutilizarNumeracao(String xmlInutAssinado) {
        operationGuard.exigirInutilizacaoPermitida();
        validarXmlAssinado(xmlInutAssinado, "infInut");
        return enviar(properties.getEndpoints().getInutilizacao(), xmlInutAssinado,
                NS_INUTILIZACAO, NS_INUTILIZACAO + "/nfeInutilizacaoNF");
    }

    public String consultarDistribuicaoDfe(String xmlConsulta) {
        exigirXml(xmlConsulta);
        return enviar(properties.getEndpoints().getDistribuicaoDfe(), xmlConsulta,
                NS_DISTRIBUICAO_DFE, NS_DISTRIBUICAO_DFE + "/nfeDistDFeInteresse");
    }

    private void validarXmlAssinado(String xml, String element) {
        exigirXml(xml);
        signatureValidator.validar(xml, element);
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

    /**
     * Envia um envelope SOAP 1.2 assinado/validado para a SEFAZ e retorna o corpo da resposta como texto.
     *
     * <p>Não loga o conteúdo do XML (dados fiscais sensíveis) — apenas metadados operacionais.</p>
     */
    private String enviar(URI endpoint, String xmlDados, String namespace, String action) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new NfeException("Endpoint HTTPS NF-e não configurado.");
        }

        String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\"><soap12:Body>"
                + "<nfeDadosMsg xmlns=\"" + namespace + "\">" + xmlDados
                + "</nfeDadosMsg></soap12:Body></soap12:Envelope>";

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(properties.getTimeoutMillis()))
                .header("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + action + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(soap, StandardCharsets.UTF_8))
                .build();

        long inicio = System.currentTimeMillis();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            long duracaoMs = System.currentTimeMillis() - inicio;
            String corpo = new String(response.body(), StandardCharsets.UTF_8);

            log.info("SEFAZ NF-e [{}] -> status={} tempoMs={}", action, response.statusCode(), duracaoMs);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NfeException("SEFAZ retornou HTTP " + response.statusCode()
                        + " para a operação " + action + ".");
            }
            if (corpo.isBlank()) {
                throw new NfeException("SEFAZ retornou HTTP " + response.statusCode() + " sem corpo.");
            }
            if (corpo.contains("soap:Fault") || corpo.contains("soap12:Fault")) {
                log.warn("SEFAZ NF-e [{}] retornou SOAP Fault.", action);
                throw new NfeException("SEFAZ retornou SOAP Fault para a operação " + action + ".");
            }
            return corpo;
        } catch (NfeException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Falha na comunicação mTLS/SOAP com a SEFAZ NF-e [{}].", action, exception);
            throw new NfeException("Falha na comunicação mTLS/SOAP com a SEFAZ NF-e.", exception);
        }
    }
}