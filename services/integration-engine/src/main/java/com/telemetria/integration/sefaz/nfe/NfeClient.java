package com.telemetria.integration.sefaz.nfe;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.CteException;

@Component("nfeClient")
public class NfeClient {

    @Value("${sefaz.nfe.url-autorizacao}")
    private String urlAutorizacao;

    @Value("${sefaz.nfe.url-consulta}")
    private String urlConsulta;

    @Value("${sefaz.nfe.url-evento}")
    private String urlEvento;

    @Value("${sefaz.nfe.url-inutilizacao}")
    private String urlInutilizacao;

    @Value("${sefaz.nfe.url-status-servico}")
    private String urlStatusServico;

    @Value("${sefaz.nfe.url-ret-autorizacao}")
    private String urlRetAutorizacao;

    @Value("${sefaz.nfe.url-distribuicao-dfe:https://www1.nfe.fazenda.gov.br/NFeDistribuicaoDFe/NFeDistribuicaoDFe.asmx}")
    private String urlDistribuicaoDfe;

    /**
     * Envia lote de NF-e/NFC-e para a SEFAZ MT
     */
    public String autorizarNfe(String xmlNfeAssinado) {
        String soapEnvelope = construirEnvelopeSoap(xmlNfeAssinado, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeAutorizacao4");
        return enviarSoap(urlAutorizacao, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeAutorizacao4/nfeAutorizacaoLote");
    }

    /**
     * Consulta o resultado do processamento de um lote enviado em modo assíncrono.
     *
     * @param nRec  número do recibo retornado no envio do lote
     * @param tpAmb 1=Produção, 2=Homologação
     */
    public String consultarReciboAutorizacao(String nRec, String tpAmb) {
        String xmlDados = String.format(
            "<consReciNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">" +
                "<tpAmb>%s</tpAmb><nRec>%s</nRec>" +
            "</consReciNFe>", tpAmb, nRec
        );
        String soapEnvelope = construirEnvelopeSoap(xmlDados, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeRetAutorizacao4");
        return enviarSoap(urlRetAutorizacao, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeRetAutorizacao4/nfeRetAutorizacaoLote");
    }

    /**
     * Consulta situação do protocolo da NF-e
     */
    public String consultarNfe(String chaveAcesso, String ambiente) {
        String xmlCons = String.format(
            "<consSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">" +
                "<tpAmb>%s</tpAmb><xServ>CONSULTAR</xServ><chNFe>%s</chNFe>" +
            "</consSitNFe>", ambiente, chaveAcesso
        );
        String soapEnvelope = construirEnvelopeSoap(xmlCons, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4");
        return enviarSoap(urlConsulta, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeConsultaProtocolo4/nfeConsultaNF");
    }

    /**
     * Consulta a disponibilidade e o tempo de resposta do servidor da SEFAZ.
     *
     * @param tpAmb 1=Produção, 2=Homologação
     * @param cUF   código IBGE do Estado (ex: 51 para Mato Grosso)
     */
    public String consultarStatusServico(String tpAmb, String cUF) {
        String xmlDados = String.format(
            "<consStatServ xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">" +
                "<tpAmb>%s</tpAmb><cUF>%s</cUF><xServ>STATUS</xServ>" +
            "</consStatServ>", tpAmb, cUF
        );
        String soapEnvelope = construirEnvelopeSoap(xmlDados, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico4");
        return enviarSoap(urlStatusServico, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico4/nfeStatusServicoNF");
    }

    /**
     * Envia evento de cancelamento de uma NF-e já autorizada.
     * O evento de cancelamento (tpEvento 110111) precisa ser assinado
     * digitalmente antes de ser enviado — este método assume que o
     * XML recebido já está assinado.
     *
     * @param xmlEventoAssinado XML do evento (envEvento) já assinado
     */
    public String cancelarNfe(String xmlEventoAssinado) {
        String soapEnvelope = construirEnvelopeSoap(xmlEventoAssinado, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4");
        return enviarSoap(urlEvento, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4/nfeRecepcaoEvento");
    }

    /**
     * Envia carta de correção eletrônica (CC-e) para uma NF-e.
     * Assim como o cancelamento, é um evento (tpEvento 110110) e
     * também assume que o XML já está assinado.
     *
     * @param xmlEventoAssinado XML do evento (envEvento) já assinado
     */
    public String enviarCartaCorrecao(String xmlEventoAssinado) {
        String soapEnvelope = construirEnvelopeSoap(xmlEventoAssinado, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4");
        return enviarSoap(urlEvento, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4/nfeRecepcaoEvento");
    }

    /**
     * Registra um evento de Manifestação do Destinatário (MD-e) para uma
     * NF-e recebida de terceiros: Ciência da Emissão (210210), Confirmação
     * da Operação (210200), Desconhecimento da Operação (210220) ou
     * Operação não Realizada (210240). Usa o mesmo endpoint de eventos.
     *
     * @param xmlEventoAssinado XML do evento de manifestação já assinado
     */
    public String manifestarDestinatario(String xmlEventoAssinado) {
        String soapEnvelope = construirEnvelopeSoap(xmlEventoAssinado, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4");
        return enviarSoap(urlEvento, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4/nfeRecepcaoEvento");
    }

    /**
     * Inutiliza uma faixa de numeração de NF-e não utilizada.
     *
     * @param xmlInutAssinado XML de inutilização (inutNFe) já assinado
     */
    public String inutilizarNumeracao(String xmlInutAssinado) {
        String soapEnvelope = construirEnvelopeSoap(xmlInutAssinado, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeInutilizacao4");
        return enviarSoap(urlInutilizacao, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NfeInutilizacao4/nfeInutilizacaoNF");
    }

    /**
     * Busca XMLs/resumos de documentos fiscais emitidos por terceiros
     * contra o seu CNPJ, via Web Service de Distribuição DF-e (ambiente
     * nacional/AN). Útil para gestão de compras/entradas por telemetria.
     *
     * @param xmlConsChaveOuNSU XML de consulta da Distribuição DF-e
     *                          (consulta por NSU, último NSU ou chave)
     */
    public String consultarDistribuicaoDfe(String xmlConsChaveOuNSU) {
        String soapEnvelope = construirEnvelopeSoap(xmlConsChaveOuNSU, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeDistribuicaoDFe");
        return enviarSoap(urlDistribuicaoDfe, soapEnvelope, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeDistribuicaoDFe/nfeDistDFeInteresse");
    }

    private String construirEnvelopeSoap(String xmlDados, String namespaceWsdl) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
               "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
               "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
               "xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" +
                 "<soap12:Body>" +
                   "<nfeDadosMsg xmlns=\"" + namespaceWsdl + "\">" +
                       xmlDados +
                   "</nfeDadosMsg>" +
                 "</soap12:Body>" +
               "</soap12:Envelope>";
    }

    private String enviarSoap(String endpoint, String soapBody, String soapAction) {
        try {
            URL url = new URL(endpoint);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + soapAction + "\"");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = soapBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            var inputStream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } catch (Exception e) {
            throw new CteException("Falha na comunicação SOAP com a SEFAZ MT: " + e.getMessage(), e);
        }
    }
}