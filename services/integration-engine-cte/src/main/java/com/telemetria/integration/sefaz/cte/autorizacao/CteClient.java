package com.telemetria.integration.sefaz.cte.autorizacao;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.security.XmlSignatureValidator;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.retorno.CteAutorizacaoResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteConsultaResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteEventoResultado;
import com.telemetria.integration.sefaz.cte.soap.CteResponseParser;
import com.telemetria.integration.sefaz.cte.soap.CteSoapEnvelopeFactory;
import com.telemetria.integration.sefaz.cte.soap.CteSoapService;
import com.telemetria.integration.sefaz.cte.soap.CteSoapTransport;
import com.telemetria.integration.sefaz.cte.validation.CteFiscalOperationGuard;
import com.telemetria.integration.sefaz.cte.validation.CteXmlValidator;

/**
 * Cliente centralizador de comunicação com SEFAZ CT-e.
 * <p>
 * Responsável por concentrar as chamadas aos webservices da SEFAZ
 * relacionadas ao Conhecimento de Transporte eletrônico (CT-e),
 * como emissão, consulta, cancelamento e inutilização de numeração.
 */
@Component
public class CteClient {

    private static final Logger log = LoggerFactory.getLogger(CteClient.class);

    private final SefazProperties sefazProperties;
    private final XmlSignatureValidator xmlSignatureValidator;
    private final CteXmlValidator cteXmlValidator;
    private final CteSoapTransport soapTransport;
    private final CteResponseParser responseParser;
    private final CteFiscalOperationGuard operationGuard;

    @Value("${sefaz.cte.timeout:30000}")
    private int timeout; // Padrão: 30 segundos

    public CteClient(SefazProperties sefazProperties, XmlSignatureValidator xmlSignatureValidator,
            CteXmlValidator cteXmlValidator, CteSoapTransport soapTransport,
            CteResponseParser responseParser, CteFiscalOperationGuard operationGuard) {
        this.sefazProperties = sefazProperties;
        this.xmlSignatureValidator = xmlSignatureValidator;
        this.cteXmlValidator = cteXmlValidator;
        this.soapTransport = soapTransport;
        this.responseParser = responseParser;
        this.operationGuard = operationGuard;
    }

    /**
     * Envia um CT-e para autorização junto à SEFAZ.
     *
     * @param xmlCte XML do CT-e assinado digitalmente
     * @return retorno bruto da SEFAZ com o resultado do processamento
     */
    public String autorizarCte(String xmlCte) {

        log.info("CT-e: iniciando autorização fiscal");

        operationGuard.exigirAutorizacaoPermitida();

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

            // Valida o leiaute oficial e a assinatura antes da transmissão.
            cteXmlValidator.validarCte(xmlCte);
            xmlSignatureValidator.validar(xmlCte, "infCte");
            log.debug("CT-e: XML e assinatura validados para autorização");

            /*
             * 3. Monta o envelope SOAP contendo o XML do CT-e
             */
            String soapRequest = CteSoapEnvelopeFactory.wrap(xmlCte, CteSoapService.AUTORIZACAO);

            /*
             * 4. Envia a requisição via HTTPS para o WebService de Recepção da SEFAZ
             */
            log.info("CT-e: transmitindo autorização à SEFAZ (payloadBytes={})",
                    xmlCte.getBytes(StandardCharsets.UTF_8).length);
            String resposta = soapTransport.enviar(
                    soapRequest, sefazProperties.getCte().getEndpoints().getAutorizacao(),
                    CteSoapService.AUTORIZACAO, timeout);
            log.info("CT-e: autorização recebeu resposta da SEFAZ (respostaBytes={})",
                    resposta.getBytes(StandardCharsets.UTF_8).length);

            /*
             * 5. Retorna a resposta bruta (XML de retorno da SEFAZ)
             */
            return resposta;

        } catch (IllegalArgumentException e) {
            log.warn("CT-e: autorização rejeitada por entrada inválida: {}", e.getMessage());
            // Propaga validações de parâmetro sem envelopar na CteException
            throw e;
        } catch (Exception e) {
            log.warn("CT-e: falha na autorização: {}", e.getMessage());
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

        log.info("CT-e: iniciando consulta de documento");

        /*
         * 1. Valida se a chave possui exatamente 44 dígitos numéricos
         */
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{44}")) {
            throw new IllegalArgumentException(
                    "A chave de acesso deve conter exatamente 44 dígitos numéricos."
            );
        }

        try {
            // tpAmb pertence ao contexto da operação, não faz parte da chave de acesso.
            String tpAmb = sefazProperties.getCte().ambienteCte().codigo();

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

            cteXmlValidator.validarConsulta(xmlConsulta);
            log.debug("CT-e: XML de consulta validado");

            /*
             * 4. Envelopa o XML da consulta dentro da estrutura SOAP 1.2
             */
            String soapRequest = CteSoapEnvelopeFactory.wrap(xmlConsulta, CteSoapService.CONSULTA);

            /*
             * 5. Transmite para o WebService de Consulta da SEFAZ
             */
            log.info("CT-e: transmitindo consulta à SEFAZ");
            String resposta = soapTransport.enviar(
                    soapRequest, sefazProperties.getCte().getEndpoints().getConsulta(),
                    CteSoapService.CONSULTA, timeout);
            log.info("CT-e: consulta recebeu resposta da SEFAZ (respostaBytes={})",
                    resposta.getBytes(StandardCharsets.UTF_8).length);
            return resposta;

        } catch (IllegalArgumentException e) {
            log.warn("CT-e: consulta rejeitada por entrada inválida: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("CT-e: falha na consulta: {}", e.getMessage());
            throw new CteException(
                    "Erro ao consultar situação do CT-e na SEFAZ.",
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
        log.info("CT-e: iniciando transmissão de evento");
        operationGuard.exigirCancelamentoPermitido();
        if (xmlEventoAssinado == null || xmlEventoAssinado.isBlank()) {
            throw new IllegalArgumentException("XML do evento assinado não pode ser vazio.");
        }
        try {
            parseXml(xmlEventoAssinado);
            cteXmlValidator.validarEvento(xmlEventoAssinado);
            xmlSignatureValidator.validar(xmlEventoAssinado, "infEvento");
            log.debug("CT-e: XML e assinatura do evento validados");
            log.info("CT-e: transmitindo evento à SEFAZ (payloadBytes={})",
                    xmlEventoAssinado.getBytes(StandardCharsets.UTF_8).length);
            String resposta = soapTransport.enviar(
                    CteSoapEnvelopeFactory.wrap(xmlEventoAssinado, CteSoapService.EVENTO),
                    sefazProperties.getCte().getEndpoints().getEvento(),
                    CteSoapService.EVENTO, timeout);
            log.info("CT-e: evento recebeu resposta da SEFAZ (respostaBytes={})",
                    resposta.getBytes(StandardCharsets.UTF_8).length);
            return resposta;
        } catch (IllegalArgumentException e) {
            log.warn("CT-e: evento rejeitado por entrada inválida: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("CT-e: falha no envio de evento: {}", e.getMessage());
            throw new CteException("Erro ao transmitir evento do CT-e para a SEFAZ.", e);
        }
    }

    public CteAutorizacaoResultado autorizarCteComResultado(String xmlCte) {
        return responseParser.parseAutorizacao(autorizarCte(xmlCte));
    }

    public CteConsultaResultado consultarCteComResultado(String chaveAcesso) {
        return responseParser.parseConsulta(consultarCte(chaveAcesso));
    }

    public CteEventoResultado enviarEventoComResultado(String xmlEventoAssinado) {
        return responseParser.parseEvento(enviarEvento(xmlEventoAssinado));
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

}
