package com.telemetria.integration.sefaz.nfe;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.telemetria.integration.security.XmlSignatureValidator;

/** Cliente SOAP 1.2 da NF-e 4.00, com mTLS e operações fiscais protegidas. */
@Component
public class NfeClient {
    private final NfeProperties properties;
    private final SSLContext sslContext;
    private final XmlSignatureValidator signatureValidator;
    private final NfeFiscalOperationGuard operationGuard;

    public NfeClient(NfeProperties properties, @Qualifier("sefazSslContext") SSLContext sslContext,
            XmlSignatureValidator signatureValidator, NfeFiscalOperationGuard operationGuard) {
        this.properties = properties;
        this.sslContext = sslContext;
        this.signatureValidator = signatureValidator;
        this.operationGuard = operationGuard;
    }

    public String autorizarNfe(String xmlNfeAssinado) {
        operationGuard.exigirAutorizacaoPermitida();
        validarXmlAssinado(xmlNfeAssinado, "infNFe");
        return enviar(properties.getEndpoints().getAutorizacao(), xmlNfeAssinado,
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeAutorizacao4",
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeAutorizacao4/nfeAutorizacaoLote");
    }

    public String consultarReciboAutorizacao(String nRec) {
        if (nRec == null || !nRec.matches("\\d{15}")) throw new IllegalArgumentException("nRec deve possuir 15 dígitos.");
        String xml = "<consReciNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><nRec>" + nRec + "</nRec></consReciNFe>";
        return enviar(properties.getEndpoints().getRetAutorizacao(), xml,
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeRetAutorizacao4",
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeRetAutorizacao4/nfeRetAutorizacaoLote");
    }

    public String consultarNfe(String chaveAcesso) {
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{44}")) {
            throw new IllegalArgumentException("A chave de acesso NF-e deve possuir 44 dígitos.");
        }
        String xml = "<consSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><xServ>CONSULTAR</xServ><chNFe>"
                + chaveAcesso + "</chNFe></consSitNFe>";
        return enviar(properties.getEndpoints().getConsulta(), xml,
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4",
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4/nfeConsultaNF");
    }

    public String consultarStatusServico() {
        String xml = "<consStatServ xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + properties.getAmbiente() + "</tpAmb><cUF>" + properties.getCodigoUf()
                + "</cUF><xServ>STATUS</xServ></consStatServ>";
        return enviar(properties.getEndpoints().getStatusServico(), xml,
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico4",
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico4/nfeStatusServicoNF");
    }

    public String enviarEvento(String xmlEventoAssinado) {
        operationGuard.exigirEventoPermitido();
        validarXmlAssinado(xmlEventoAssinado, "infEvento");
        return enviar(properties.getEndpoints().getEvento(), xmlEventoAssinado,
                "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4",
                "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4/nfeRecepcaoEvento");
    }

    public String inutilizarNumeracao(String xmlInutAssinado) {
        operationGuard.exigirInutilizacaoPermitida();
        validarXmlAssinado(xmlInutAssinado, "infInut");
        return enviar(properties.getEndpoints().getInutilizacao(), xmlInutAssinado,
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeInutilizacao4",
                "http://www.portalfiscal.inf.br/nfe/wsdl/NfeInutilizacao4/nfeInutilizacaoNF");
    }

    public String consultarDistribuicaoDfe(String xmlConsulta) {
        exigirXml(xmlConsulta);
        return enviar(properties.getEndpoints().getDistribuicaoDfe(), xmlConsulta,
                "http://www.portalfiscal.inf.br/nfe/wsdl/NFeDistribuicaoDFe",
                "http://www.portalfiscal.inf.br/nfe/wsdl/NFeDistribuicaoDFe/nfeDistDFeInteresse");
    }

    private void validarXmlAssinado(String xml, String element) {
        exigirXml(xml);
        signatureValidator.validar(xml, element);
    }
    private void exigirXml(String xml) {
        if (xml == null || xml.isBlank()) throw new IllegalArgumentException("XML NF-e não pode ser vazio.");
    }

    private String enviar(URI endpoint, String xmlDados, String namespace, String action) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new NfeException("Endpoint HTTPS NF-e não configurado.");
        }
        String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\"><soap12:Body>"
                + "<nfeDadosMsg xmlns=\"" + namespace + "\">" + xmlDados
                + "</nfeDadosMsg></soap12:Body></soap12:Envelope>";
        try {
            HttpsURLConnection connection = (HttpsURLConnection) endpoint.toURL().openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(properties.getTimeoutMillis());
            connection.setReadTimeout(properties.getTimeoutMillis());
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + action + "\"");
            try (OutputStream output = connection.getOutputStream()) { output.write(soap.getBytes(StandardCharsets.UTF_8)); }
            int status = connection.getResponseCode();
            InputStream input = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            if (input == null) throw new NfeException("SEFAZ retornou HTTP " + status + " sem corpo.");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().reduce("", String::concat);
            }
        } catch (NfeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NfeException("Falha na comunicação mTLS/SOAP com a SEFAZ NF-e.", exception);
        }
    }
}
