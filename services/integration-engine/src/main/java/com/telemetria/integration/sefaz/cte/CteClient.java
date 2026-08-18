package com.telemetria.integration.sefaz.cte;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Cliente centralizador de comunicação com SEFAZ CT-e.
 * <p>
 * Responsável por concentrar as chamadas aos webservices da SEFAZ
 * relacionadas ao Conhecimento de Transporte eletrônico (CT-e),
 * como emissão, consulta, cancelamento e inutilização de numeração.
 */
@Component
public class CteClient {

    // Atributos injetados via application.properties / application.yml
    @Value("${sefaz.cte.url-webservice:${SEFAZ_CTE_WEBSERVICE_URL:https://homologacao.sefaz.mt.gov.br/ctews2/services/CTeRecepcaoEventoV4}}")
    private String urlWebservice;

    @Value("${sefaz.certificado.arquivo:}")
    private String certificadoPath;

    @Value("${sefaz.certificado.senha:}")
    private String certificadoSenha;

    @Value("${sefaz.cte.timeout:30000}")
    private int timeout; // Padrão: 30 segundos

    /**
     * Envia um CT-e para autorização junto à SEFAZ.
     *
     * @param xmlCte XML do CT-e assinado digitalmente
     * @return retorno bruto da SEFAZ com o resultado do processamento
     */
    public String autorizarCte(String xmlCte) {

        if (xmlCte == null || xmlCte.isBlank()) {
            throw new IllegalArgumentException(
                    "XML do CT-e não pode ser vazio."
            );
        }

        try {
            /*
             * 1. Valida se a estrutura DOM do XML é válida
             */
            Document document = parseXml(xmlCte);

            /*
             * 2. Extrai a tag <CTe> para garantir que o documento correto foi informado
             */
            NodeList cteNodes = document.getElementsByTagName("CTe");

            if (cteNodes.getLength() == 0) {
                throw new IllegalArgumentException(
                        "O XML informado não contém a tag CTe."
                );
            }

            /*
             * 3. Monta o envelope SOAP contendo o XML do CT-e
             */
            String soapRequest = montarRequisicaoSoap(xmlCte);

            /*
             * 4. Envia a requisição via HTTPS para o WebService de Recepção da SEFAZ
             */
            String resposta = enviarParaSefaz(soapRequest);

            /*
             * 5. Retorna a resposta bruta (XML de retorno da SEFAZ)
             */
            return resposta;

        } catch (IllegalArgumentException e) {
            // Propaga validações de parâmetro sem envelopar na CteException
            throw e;
        } catch (Exception e) {
            throw new CteException(
                    "Erro ao enviar CT-e para autorização na SEFAZ.",
                    e
            );
        }
    }

    /**
     * Consulta a situação de um CT-e já processado pela SEFAZ.
     *
     * @param chaveAcesso chave de acesso do CT-e (44 dígitos)
     * @return retorno bruto da SEFAZ com a situação do documento
     */
    public String consultarCte(String chaveAcesso) {

        /*
         * 1. Valida se a chave possui exatamente 44 dígitos numéricos
         */
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{44}")) {
            throw new IllegalArgumentException(
                    "A chave de acesso deve conter exatamente 44 dígitos numéricos."
            );
        }

        try {
            /*
             * 2. Extrai o tipo de ambiente (tpAmb) direto da chave de acesso.
             * O 35º dígito da chave indica o ambiente:
             * '1' = Produção
             * '2' = Homologação
             */
            String tpAmb = String.valueOf(chaveAcesso.charAt(34));

            /*
             * 3. Monta o XML de consulta (consSitCTe v4.00)
             */
            String xmlConsulta = """
                    <consSitCTe versao="4.00" xmlns="http://www.portalfiscal.inf.br/cte">
                        <tpAmb>%s</tpAmb>
                        <xServ>CONSULTAR</xServ>
                        <chCTe>%s</chCTe>
                    </consSitCTe>
                    """.formatted(tpAmb, chaveAcesso).trim();

            /*
             * 4. Envelopa o XML da consulta dentro da estrutura SOAP 1.2
             */
            String soapRequest = montarRequisicaoSoap(xmlConsulta);

            /*
             * 5. Transmite para o WebService de Consulta da SEFAZ
             */
            return enviarParaSefaz(soapRequest);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException(
                    "Erro ao consultar situação do CT-e na SEFAZ.",
                    e
            );
        }
    }

    /**
     * Solicita o cancelamento de um CT-e já autorizado.
     *
     * @param chaveAcesso chave de acesso do CT-e (44 dígitos)
     * @param justificativa motivo do cancelamento (mínimo 15 caracteres)
     * @return retorno bruto da SEFAZ com o resultado do cancelamento
     */
    public String cancelarCte(String chaveAcesso, String justificativa) {

        // 1. Validação da Chave de Acesso
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{44}")) {
            throw new IllegalArgumentException(
                    "A chave de acesso deve conter exatamente 44 dígitos numéricos."
            );
        }

        // 2. Validação da Justificativa (Mínimo de 15 caracteres exigido pela SEFAZ)
        if (justificativa == null || justificativa.trim().length() < 15) {
            throw new IllegalArgumentException(
                    "A justificativa de cancelamento deve conter no mínimo 15 caracteres."
            );
        }

        try {
            // 3. Extrai o tipo de ambiente (tpAmb) do 35º dígito da chave (índice 34)
            String tpAmb = String.valueOf(chaveAcesso.charAt(34));
            
            // O CNPJ do emitente pode ser extraído diretamente da chave (posições 6 a 19 - índices 6 a 20)
            String cnpjEmitente = chaveAcesso.substring(6, 20);

            // Identificador do evento segue o padrão: ID + tipo_evento (110111 = Cancelamento) + chave + lote (ex: 01)
            String idEvento = "ID110111" + chaveAcesso + "01";
            
            // Data e hora atual no formato padrão exigido pela SEFAZ (AAAA-MM-DDThh:mm:ssTZD)
            String dhEvento = java.time.OffsetDateTime.now().format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            // 4. Monta o XML do Evento de Cancelamento (Layout v4.00)
            String xmlEvento = """
                    <envEvento versao="4.00" xmlns="http://www.portalfiscal.inf.br/cte">
                        <idLote>1</idLote>
                        <evento versao="4.00">
                            <infEvento Id="%s">
                                <tpAmb>%s</tpAmb>
                                <cOrgao>%s</cOrgao>
                                <CNPJ>%s</CNPJ>
                                <chCTe>%s</chCTe>
                                <dhEvento>%s</dhEvento>
                                <tpEvento>110111</tpEvento>
                                <nSeqEvento>1</nSeqEvento>
                                <detEvento versao="4.00">
                                    <evCancCTe versao="4.00">
                                        <descEvento>Cancelamento</descEvento>
                                        <nProt>NUMERO_DO_PROTOCOLO_DE_AUTORIZACAO</nProt>
                                        <xJust>%s</xJust>
                                    </evCancCTe>
                                </detEvento>
                            </infEvento>
                        </evento>
                    </envEvento>
                    """.formatted(
                            idEvento,
                            tpAmb,
                            chaveAcesso.substring(0, 2), // cOrgao (2 primeiros dígitos da chave)
                            cnpjEmitente,
                            chaveAcesso,
                            dhEvento,
                            justificativa.trim()
                    ).trim();

            /*
             * Nota importante: Na prática, a tag <infEvento> e o <evento> precisam ser assinados
             * digitalmente com o Certificado A1 antes de serem enviados à SEFAZ. 
             * Se o xmlEvento for enviado puro, a SEFAZ rejeitará por falta de assinatura.
             */

            // 5. Encapsula o XML do evento no Envelope SOAP 1.2
            String soapRequest = montarRequisicaoSoap(xmlEvento);

            // 6. Transmite para o WebService de Recepção de Eventos da SEFAZ
            return enviarParaSefaz(soapRequest);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException(
                    "Erro ao solicitar cancelamento do CT-e na SEFAZ.",
                    e
            );
        }
    }

    /**
     * Transmite um evento CT-e que já foi montado e assinado digitalmente.
     *
     * @param xmlEventoAssinado XML do evento contendo a assinatura XMLDSig
     * @return retorno bruto da SEFAZ
     */
    public String enviarEvento(String xmlEventoAssinado) {
        if (xmlEventoAssinado == null || xmlEventoAssinado.isBlank()) {
            throw new IllegalArgumentException("XML do evento assinado não pode ser vazio.");
        }
        if (!xmlEventoAssinado.contains("<ds:Signature") && !xmlEventoAssinado.contains("<Signature")) {
            throw new IllegalArgumentException("XML do evento não contém assinatura digital.");
        }

        try {
            parseXml(xmlEventoAssinado);
            return enviarParaSefaz(montarRequisicaoSoap(xmlEventoAssinado));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Erro ao transmitir evento do CT-e para a SEFAZ.", e);
        }
    }

    /**
     * Inutiliza uma faixa de numeração de CT-e não utilizada.
     * <p>
     * <b>Atenção:</b> Na versão 4.00 do CT-e, o serviço de inutilização de numeração 
     * foi descontinuado e extinto pela SEFAZ. Números pulados devem ser geridos 
     * diretamente na escrituração fiscal (EFD).
     *
     * @param serie série do CT-e
     * @param numeroInicial número inicial da faixa
     * @param numeroFinal número final da faixa
     * @param justificativa motivo da inutilização
     * @return retorno bruto da SEFAZ com o resultado da inutilização
     * @deprecated O serviço de inutilização de numeração foi extinto no CT-e v4.00.
     */
    @Deprecated
    public String inutilizarNumeracao(int serie, int numeroInicial, int numeroFinal, String justificativa) {
        throw new UnsupportedOperationException(
                "O WebService de Inutilização de Numeração (CteInutilizacao) foi extinto na versão 4.00 do CT-e. " +
                "Numerações não utilizadas não precisam mais ser homologadas via webservice na SEFAZ."
        );
    }

    /**
     * Verifica o status do serviço da SEFAZ para o CT-e.
     * <p>
     * <b>Atenção:</b> O WebService de Status do Serviço (CteStatusServico) foi extinto 
     * na versão 4.00 do CT-e. A verificação prévia de disponibilidade não é mais necessária 
     * nem suportada pela SEFAZ.
     *
     * @return retorno bruto da SEFAZ com o status do serviço
     * @deprecated Endpoint extinto no CT-e v4.00.
     */
    @Deprecated
    public String verificarStatusServico() {
        throw new UnsupportedOperationException(
                "O WebService de Status do Serviço (CteStatusServico) foi extinto na versão 4.00 do CT-e. " +
                "As requisições devem ser enviadas diretamente para os serviços de autorização/consulta."
        );
    }

    /* ========================================================================
     * Métodos Auxiliares e Utilitários (Privados)
     * ======================================================================== */

    /**
     * Converte o XML recebido em um Document DOM.
     */
    private Document parseXml(String xml) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        /*
         * Proteções básicas contra XML External Entity (XXE).
         */
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
        );

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory
                .newDocumentBuilder()
                .parse(
                        new ByteArrayInputStream(
                                xml.getBytes(StandardCharsets.UTF_8)
                        )
                );
    }

    /**
     * Monta a requisição SOAP.
     *
     * IMPORTANTE:
     * A estrutura abaixo é uma base.
     * O namespace e a operação precisam ser ajustados
     * conforme o WebService da SEFAZ utilizado.
     */
    private String montarRequisicaoSoap(String xmlCte) {

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soap12:Envelope
                    xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">

                    <soap12:Header/>

                    <soap12:Body>

                        <cteDadosMsg>

                            %s

                        </cteDadosMsg>

                    </soap12:Body>

                </soap12:Envelope>
                """.formatted(xmlCte);
    }

    /**
     * Executa a chamada HTTP/SOAP para a SEFAZ.
     *
     * @param soapRequest envelope SOAP montado no formato XML
     * @return retorno bruto em XML vindo da SEFAZ
     */
    private String enviarParaSefaz(String soapRequest) {
        if (soapRequest == null || soapRequest.isBlank()) {
            throw new IllegalArgumentException("O payload SOAP não pode ser nulo ou vazio.");
        }

        try {
            // 1. Configura o contexto SSL com o Certificado Digital A1 (.pfx / .p12)
            SSLContext sslContext = criarSSLContext();

            // 2. Abre a conexão HTTPS com a URL do WebService da SEFAZ
            URL url = new URL(this.urlWebservice);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(this.timeout);
            conn.setReadTimeout(this.timeout);

            // 3. Define os cabeçalhos do SOAP 1.2
            conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");

            // 4. Envia o envelope SOAP
            byte[] postData = soapRequest.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(postData.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData, 0, postData.length);
                os.flush();
            }

            // 5. Captura o status da resposta HTTP (200 = OK)
            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            // 6. Lê e retorna a resposta XML da SEFAZ
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder resposta = new StringBuilder();
                String linha;
                while ((linha = br.readLine()) != null) {
                    resposta.append(linha);
                }
                return resposta.toString();
            }

        } catch (Exception e) {
            throw new CteException("Falha na comunicação HTTPS/SOAP com a SEFAZ: " + e.getMessage(), e);
        }
    }

    /**
     * Cria e inicializa o SSLContext (TLSv1.2) utilizando o arquivo do certificado digital A1.
     */
    private SSLContext criarSSLContext() throws Exception {
        // Carrega o repositório PKCS12 (.pfx / .p12)
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream ksStream = new FileInputStream(this.certificadoPath)) {
            keyStore.load(ksStream, this.certificadoSenha.toCharArray());
        }

        // Inicializa o KeyManager responsável por apresentar o certificado do cliente à SEFAZ
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, this.certificadoSenha.toCharArray());

        // Inicializa o TrustManager nativo do Java para validação da cadeia de certificação
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);

        // Força a utilização do protocolo TLSv1.2 exigido pela SEFAZ
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }
}
