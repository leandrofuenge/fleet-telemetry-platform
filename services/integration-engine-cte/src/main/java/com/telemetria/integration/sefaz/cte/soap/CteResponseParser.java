package com.telemetria.integration.sefaz.cte.soap;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.telemetria.integration.sefaz.cte.domain.CteResultadoParse;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.retorno.CteAutorizacaoResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteCodigoRetorno;
import com.telemetria.integration.sefaz.cte.retorno.CteConsultaResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteEventoResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteOperacao;
import com.telemetria.integration.sefaz.cte.retorno.CteSoapFaultException;
import com.telemetria.integration.sefaz.cte.retorno.CteStatusResultado;

/**
 * Parser de respostas SOAP/XML do CT-e 4.00.
 *
 * Responsabilidades:
 *
 * - transformar XML SEFAZ em objetos de domínio;
 * - detectar SOAP Fault;
 * - impedir XXE;
 * - validar campos obrigatórios;
 * - identificar automaticamente o tipo de retorno;
 * - evitar parsing duplicado do mesmo XML.
 *
 * Esta implementação trabalha com um único CT-e por resposta de autorização.
 */
@Component
public class CteResponseParser {

    /*
     * ==========================================================
     * AUTORIZAÇÃO
     * ==========================================================
     */

    public CteAutorizacaoResultado parseAutorizacao(String xml) {

        Document document = prepararDocumento(xml);

        return parseAutorizacao(document, xml);
    }

    private CteAutorizacaoResultado parseAutorizacao(
            Document document,
            String xml) {

        /*
         * No retorno de autorização normalmente teremos:
         *
         * retEnviCTe
         *   protCTe
         *     infProt
         */
        Element contexto = primeiro(document, "infProt");

        if (contexto == null) {

            /*
             * Pode existir um retorno de lote sem infProt.
             * Nesse caso tentamos utilizar retEnviCTe.
             */
            contexto = primeiro(document, "retEnviCTe");
        }

        if (contexto == null) {
            throw estruturaInvalida(
                    "autorização",
                    "infProt/retEnviCTe");
        }

        int codigo = inteiroObrigatorio(
                contexto,
                "cStat");

        return new CteAutorizacaoResultado(
                codigo,
                texto(contexto, "xMotivo"),
                CteCodigoRetorno.classificar(
                        CteOperacao.AUTORIZACAO,
                        codigo),
                texto(contexto, "nProt"),
                texto(contexto, "chCTe"),
                texto(contexto, "dhRecbto"),
                texto(contexto, "digVal"),
                xml);
    }

    /*
     * ==========================================================
     * CONSULTA
     * ==========================================================
     */

    public CteConsultaResultado parseConsulta(String xml) {

        Document document = prepararDocumento(xml);

        return parseConsulta(document, xml);
    }

    private CteConsultaResultado parseConsulta(
            Document document,
            String xml) {

        Element contexto =
                primeiro(document, "retConsSitCTe");

        if (contexto == null) {
            throw estruturaInvalida(
                    "consulta",
                    "retConsSitCTe");
        }

        int codigo =
                inteiroObrigatorio(
                        contexto,
                        "cStat");

        return new CteConsultaResultado(
                codigo,
                texto(contexto, "xMotivo"),
                CteCodigoRetorno.classificar(
                        CteOperacao.CONSULTA,
                        codigo),
                texto(contexto, "nProt"),
                texto(contexto, "chCTe"),
                texto(contexto, "dhRecbto"),
                xml);
    }

    /*
     * ==========================================================
     * EVENTO
     * ==========================================================
     */

    public CteEventoResultado parseEvento(String xml) {

        Document document = prepararDocumento(xml);

        return parseEvento(document, xml);
    }

    private CteEventoResultado parseEvento(
            Document document,
            String xml) {

        Element lote =
                primeiro(document, "retEventoCTe");

        if (lote == null) {
            throw estruturaInvalida(
                    "evento",
                    "retEventoCTe");
        }

        Element evento =
                primeiro(lote, "infEvento");

        int codigoLote =
                inteiroObrigatorioDireto(
                        lote,
                        "cStat");

        int codigoEvento;

        if (evento != null) {
            codigoEvento =
                    inteiroObrigatorio(
                            evento,
                            "cStat");
        } else {
            codigoEvento = codigoLote;
        }

        String motivoLote =
                textoDireto(
                        lote,
                        "xMotivo");

        String motivoEvento =
                evento != null
                        ? texto(evento, "xMotivo")
                        : motivoLote;

        return new CteEventoResultado(
                codigoLote,
                motivoLote,
                codigoEvento,
                motivoEvento,
                CteCodigoRetorno.classificar(
                        CteOperacao.EVENTO,
                        codigoEvento),
                evento != null
                        ? texto(evento, "nProt")
                        : null,
                evento != null
                        ? texto(evento, "chCTe")
                        : null,
                evento != null
                        ? texto(evento, "tpEvento")
                        : null,
                evento != null
                        ? inteiroOpcional(
                                evento,
                                "nSeqEvento")
                        : null,
                evento != null
                        ? texto(
                                evento,
                                "dhRegEvento")
                        : null,
                xml);
    }

    /*
     * ==========================================================
     * STATUS
     * ==========================================================
     */

    public CteStatusResultado parseStatus(String xml) {

        Document document = prepararDocumento(xml);

        return parseStatus(document, xml);
    }

    private CteStatusResultado parseStatus(
            Document document,
            String xml) {

        Element contexto =
                primeiro(
                        document,
                        "retConsStatServCTe");

        if (contexto == null) {
            throw estruturaInvalida(
                    "status do serviço",
                    "retConsStatServCTe");
        }

        int codigo =
                inteiroObrigatorio(
                        contexto,
                        "cStat");

        return new CteStatusResultado(
                codigo,
                texto(contexto, "xMotivo"),
                CteCodigoRetorno.classificar(
                        CteOperacao.STATUS,
                        codigo),
                texto(contexto, "tpAmb"),
                texto(contexto, "cUF"),
                texto(contexto, "verAplic"),
                texto(contexto, "dhRecbto"),
                inteiroOpcional(
                        contexto,
                        "tMed"),
                xml);
    }

    /*
     * ==========================================================
     * DETECÇÃO AUTOMÁTICA
     * ==========================================================
     */

    /**
     * Compatibilidade com rotas antigas.
     *
     * O XML é parseado UMA ÚNICA VEZ.
     */
    public CteResultadoParse parseRetorno(String xml) {

        Document document =
                prepararDocumento(xml);

        if (existe(
                document,
                "retConsStatServCTe")) {

            CteStatusResultado result =
                    parseStatus(
                            document,
                            xml);

            return new CteResultadoParse(
                    String.valueOf(
                            result.codigo()),
                    result.motivo(),
                    null);
        }

        if (existe(
                document,
                "retConsSitCTe")) {

            CteConsultaResultado result =
                    parseConsulta(
                            document,
                            xml);

            return new CteResultadoParse(
                    String.valueOf(
                            result.codigo()),
                    result.motivo(),
                    result.protocolo());
        }

        if (existe(
                document,
                "retEventoCTe")) {

            CteEventoResultado result =
                    parseEvento(
                            document,
                            xml);

            return new CteResultadoParse(
                    String.valueOf(
                            result.codigoEvento()),
                    result.motivoEvento(),
                    result.protocoloEvento());
        }

        /*
         * Autorização.
         */
        if (existe(document, "retEnviCTe")
                || existe(document, "protCTe")
                || existe(document, "infProt")) {

            CteAutorizacaoResultado result =
                    parseAutorizacao(
                            document,
                            xml);

            return new CteResultadoParse(
                    String.valueOf(
                            result.codigo()),
                    result.motivo(),
                    result.protocolo());
        }

        /*
         * Não assumimos mais que XML desconhecido
         * seja uma autorização.
         */
        throw new CteException(
                "Tipo de resposta CT-e não reconhecido. "
                        + "Nenhum elemento de retorno conhecido foi encontrado.");
    }

    /*
     * ==========================================================
     * PARSER XML
     * ==========================================================
     */

    /**
     * Converte o XML em DOM seguro e interrompe o fluxo imediatamente
     * quando a resposta contém um SOAP Fault.
     */
    private Document prepararDocumento(String xml) {

        Document document =
                parseDocument(xml);

        validarSoapFault(document);

        return document;
    }

    private Document parseDocument(String xml) {

        validarXml(xml);

        try {

            DocumentBuilderFactory factory =
                    criarFactorySegura();

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            return builder.parse(
                    new ByteArrayInputStream(
                            xml.getBytes(
                                    StandardCharsets.UTF_8)));

        } catch (CteException e) {

            throw e;

        } catch (Exception e) {

            throw new CteException(
                    "Resposta XML inválida da SEFAZ.",
                    e);
        }
    }

    /**
     * Criamos uma factory por parsing.
     *
     * O custo é pequeno comparado à chamada HTTPS/mTLS
     * realizada anteriormente e evitamos depender de
     * garantias de thread-safety da implementação JAXP.
     */
    private DocumentBuilderFactory criarFactorySegura() {

        try {

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            /*
             * Processamento seguro.
             */
            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true);

            /*
             * Impede DOCTYPE.
             */
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);

            /*
             * Bloqueia entidades externas.
             */
            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false);

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false);

            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);

            /*
             * Proteções JAXP adicionais.
             */
            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    "");

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    "");

            factory.setXIncludeAware(false);

            factory.setExpandEntityReferences(false);

            return factory;

        } catch (ParserConfigurationException e) {

            throw new IllegalStateException(
                    "Não foi possível configurar o parser XML seguro do CT-e.",
                    e);

        } catch (IllegalArgumentException e) {

            throw new IllegalStateException(
                    "Implementação XML da JVM não suporta as "
                            + "configurações de segurança necessárias.",
                    e);
        }
    }

    /*
     * ==========================================================
     * SOAP FAULT
     * ==========================================================
     */

    private void validarSoapFault(
            Document document) {

        Element fault =
                primeiro(
                        document,
                        "Fault");

        if (fault == null) {
            return;
        }

        String codigo =
                extrairFaultCode(fault);

        String motivo =
                extrairFaultReason(fault);

        throw new CteSoapFaultException(
                codigo,
                motivo);
    }

    private String extrairFaultCode(
            Element fault) {

        /*
         * SOAP 1.2:
         *
         * Fault
         *   Code
         *     Value
         */
        Element code =
                primeiro(
                        fault,
                        "Code");

        if (code != null) {

            String value =
                    texto(
                            code,
                            "Value");

            if (temTexto(value)) {
                return value;
            }
        }

        /*
         * SOAP 1.1:
         *
         * faultcode
         */
        String faultCode =
                texto(
                        fault,
                        "faultcode");

        return primeiroNaoVazio(
                faultCode,
                "SOAP-FAULT");
    }

    private String extrairFaultReason(
            Element fault) {

        /*
         * SOAP 1.2:
         *
         * Fault
         *   Reason
         *     Text
         */
        Element reason =
                primeiro(
                        fault,
                        "Reason");

        if (reason != null) {

            String text =
                    texto(
                            reason,
                            "Text");

            if (temTexto(text)) {
                return text;
            }
        }

        /*
         * SOAP 1.1.
         */
        String faultString =
                texto(
                        fault,
                        "faultstring");

        return primeiroNaoVazio(
                faultString,
                "Falha SOAP sem descrição");
    }

    /*
     * ==========================================================
     * LEITURA DOM
     * ==========================================================
     */

    private Element primeiro(
            Node contexto,
            String nome) {

        if (contexto == null) {
            return null;
        }

        NodeList nodes;

        if (contexto instanceof Document document) {

            nodes =
                    document.getElementsByTagNameNS(
                            "*",
                            nome);

        } else if (contexto instanceof Element element) {

            nodes =
                    element.getElementsByTagNameNS(
                            "*",
                            nome);

        } else {

            return null;
        }

        if (nodes.getLength() == 0) {
            return null;
        }

        Node node =
                nodes.item(0);

        return node instanceof Element element
                ? element
                : null;
    }

    private boolean existe(
            Node contexto,
            String nome) {

        return primeiro(
                contexto,
                nome) != null;
    }

    private String texto(
            Node contexto,
            String nome) {

        Element element =
                primeiro(
                        contexto,
                        nome);

        return textoElemento(
                element);
    }

    /**
     * Busca somente filho direto.
     *
     * Útil quando existem vários cStat/xMotivo
     * em níveis diferentes do retorno.
     */
    private String textoDireto(
            Element contexto,
            String nome) {

        if (contexto == null) {
            return null;
        }

        for (Node node =
                     contexto.getFirstChild();
             node != null;
             node = node.getNextSibling()) {

            if (!(node instanceof Element element)) {
                continue;
            }

            String localName =
                    element.getLocalName();

            String nodeName =
                    element.getNodeName();

            boolean corresponde =
                    nome.equals(localName)
                            || nome.equals(nodeName);

            if (corresponde) {
                return textoElemento(
                        element);
            }
        }

        return null;
    }

    private String textoElemento(
            Element element) {

        if (element == null) {
            return null;
        }

        String value =
                element.getTextContent();

        if (value == null) {
            return null;
        }

        value = value.trim();

        return value.isEmpty()
                ? null
                : value;
    }

    /*
     * ==========================================================
     * NÚMEROS
     * ==========================================================
     */

    /**
     * cStat é obrigatório.
     *
     * Não utilizamos 999 como fallback porque uma resposta
     * malformada não deve ser confundida com um código SEFAZ.
     */
    private int inteiroObrigatorio(
            Node contexto,
            String nome) {

        String value =
                texto(
                        contexto,
                        nome);

        return converterInteiroObrigatorio(
                value,
                nome);
    }

    private int inteiroObrigatorioDireto(
            Element contexto,
            String nome) {

        String value =
                textoDireto(
                        contexto,
                        nome);

        return converterInteiroObrigatorio(
                value,
                nome);
    }

    private Integer inteiroOpcional(
            Node contexto,
            String nome) {

        String value =
                texto(
                        contexto,
                        nome);

        if (!temTexto(value)) {
            return null;
        }

        try {

            return Integer.valueOf(
                    value);

        } catch (NumberFormatException e) {

            throw campoNumericoInvalido(
                    nome,
                    value,
                    e);
        }
    }

    private int converterInteiroObrigatorio(
            String value,
            String nome) {

        if (!temTexto(value)) {

            throw new CteException(
                    "Campo obrigatório '"
                            + nome
                            + "' não encontrado na resposta da SEFAZ.");
        }

        try {

            return Integer.parseInt(
                    value);

        } catch (NumberFormatException e) {

            throw campoNumericoInvalido(
                    nome,
                    value,
                    e);
        }
    }

    /*
     * ==========================================================
     * VALIDAÇÕES
     * ==========================================================
     */

    private void validarXml(
            String xml) {

        if (xml == null
                || xml.isBlank()) {

            throw new CteException(
                    "Resposta XML da SEFAZ não pode ser vazia.");
        }
    }

    private CteException estruturaInvalida(
            String operacao,
            String elementoEsperado) {

        return new CteException(
                "Resposta da SEFAZ inválida para "
                        + operacao
                        + ": elemento '"
                        + elementoEsperado
                        + "' não encontrado.");
    }

    private CteException campoNumericoInvalido(
            String nome,
            String value,
            Exception cause) {

        return new CteException(
                "Campo '"
                        + nome
                        + "' com valor numérico inválido "
                        + "na resposta da SEFAZ: '"
                        + value
                        + "'.",
                cause);
    }

    /*
     * ==========================================================
     * UTILITÁRIOS
     * ==========================================================
     */

    private String primeiroNaoVazio(
            String... values) {

        for (String value : values) {

            if (temTexto(value)) {
                return value.trim();
            }
        }

        return null;
    }

    private boolean temTexto(
            String value) {

        return value != null
                && !value.isBlank();
    }
}
