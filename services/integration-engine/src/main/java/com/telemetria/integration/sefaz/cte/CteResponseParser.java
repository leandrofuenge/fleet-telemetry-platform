package com.telemetria.integration.sefaz.cte;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.telemetria.integration.sefaz.cte.retorno.CteAutorizacaoResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteCodigoRetorno;
import com.telemetria.integration.sefaz.cte.retorno.CteConsultaResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteEventoResultado;
import com.telemetria.integration.sefaz.cte.retorno.CteOperacao;
import com.telemetria.integration.sefaz.cte.retorno.CteSoapFaultException;
import com.telemetria.integration.sefaz.cte.retorno.CteStatusResultado;

/** Parser seguro e específico para cada retorno SOAP CT-e 4.00. */
@Component
public class CteResponseParser {

    public CteAutorizacaoResultado parseAutorizacao(String xml) {
        Document document = parse(xml);
        Element contexto = primeiro(document, "infProt");
        if (contexto == null) {
            contexto = document.getDocumentElement();
        }
        int codigo = inteiro(contexto, "cStat", 999);
        return new CteAutorizacaoResultado(
                codigo,
                texto(contexto, "xMotivo"),
                CteCodigoRetorno.classificar(CteOperacao.AUTORIZACAO, codigo),
                texto(contexto, "nProt"),
                texto(contexto, "chCTe"),
                texto(contexto, "dhRecbto"),
                texto(contexto, "digVal"),
                xml);
    }

    public CteConsultaResultado parseConsulta(String xml) {
        Document document = parse(xml);
        Element contexto = primeiro(document, "retConsSitCTe");
        if (contexto == null) {
            contexto = document.getDocumentElement();
        }
        int codigo = inteiro(contexto, "cStat", 999);
        return new CteConsultaResultado(
                codigo,
                texto(contexto, "xMotivo"),
                CteCodigoRetorno.classificar(CteOperacao.CONSULTA, codigo),
                texto(contexto, "nProt"),
                texto(contexto, "chCTe"),
                texto(contexto, "dhRecbto"),
                xml);
    }

    public CteEventoResultado parseEvento(String xml) {
        Document document = parse(xml);
        Element lote = primeiro(document, "retEventoCTe");
        if (lote == null) {
            lote = document.getDocumentElement();
        }
        Element evento = primeiro(lote, "infEvento");
        int codigoLote = inteiroDireto(lote, "cStat", 999);
        int codigoEvento = evento != null ? inteiro(evento, "cStat", codigoLote) : codigoLote;
        return new CteEventoResultado(
                codigoLote,
                textoDireto(lote, "xMotivo"),
                codigoEvento,
                evento != null ? texto(evento, "xMotivo") : textoDireto(lote, "xMotivo"),
                CteCodigoRetorno.classificar(CteOperacao.EVENTO, codigoEvento),
                evento != null ? texto(evento, "nProt") : null,
                evento != null ? texto(evento, "chCTe") : null,
                evento != null ? texto(evento, "tpEvento") : null,
                evento != null ? inteiroOpcional(evento, "nSeqEvento") : null,
                evento != null ? texto(evento, "dhRegEvento") : null,
                xml);
    }

    public CteStatusResultado parseStatus(String xml) {
        Document document = parse(xml);
        Element contexto = primeiro(document, "retConsStatServCTe");
        if (contexto == null) {
            contexto = document.getDocumentElement();
        }
        int codigo = inteiro(contexto, "cStat", 999);
        return new CteStatusResultado(
                codigo,
                texto(contexto, "xMotivo"),
                CteCodigoRetorno.classificar(CteOperacao.STATUS, codigo),
                texto(contexto, "tpAmb"),
                texto(contexto, "cUF"),
                texto(contexto, "verAplic"),
                texto(contexto, "dhRecbto"),
                inteiroOpcional(contexto, "tMed"),
                xml);
    }

    /** Compatibilidade com rotas antigas; detecta a operação pela raiz do retorno. */
    public CteResultadoParse parseRetorno(String xml) {
        Document document = parse(xml);
        if (primeiro(document, "retConsStatServCTe") != null) {
            CteStatusResultado result = parseStatus(xml);
            return new CteResultadoParse(String.valueOf(result.codigo()), result.motivo(), null);
        }
        if (primeiro(document, "retConsSitCTe") != null) {
            CteConsultaResultado result = parseConsulta(xml);
            return new CteResultadoParse(String.valueOf(result.codigo()), result.motivo(), result.protocolo());
        }
        if (primeiro(document, "retEventoCTe") != null || primeiro(document, "infEvento") != null) {
            CteEventoResultado result = parseEvento(xml);
            return new CteResultadoParse(String.valueOf(result.codigoEvento()),
                    result.motivoEvento(), result.protocoloEvento());
        }
        CteAutorizacaoResultado result = parseAutorizacao(xml);
        return new CteResultadoParse(String.valueOf(result.codigo()), result.motivo(), result.protocolo());
    }

    private Document parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new CteException("Resposta XML da SEFAZ não pode ser vazia.");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element fault = primeiro(document, "Fault");
            if (fault != null) {
                String code = primeiroNaoVazio(texto(fault, "Value"), texto(fault, "faultcode"), "SOAP-FAULT");
                String reason = primeiroNaoVazio(texto(fault, "Text"), texto(fault, "faultstring"),
                        "Falha SOAP sem descrição");
                throw new CteSoapFaultException(code, reason);
            }
            return document;
        } catch (CteException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Resposta XML inválida da SEFAZ.", e);
        }
    }

    private Element primeiro(Node contexto, String nome) {
        NodeList nodes = contexto instanceof Document document
                ? document.getElementsByTagNameNS("*", nome)
                : ((Element) contexto).getElementsByTagNameNS("*", nome);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private String texto(Node contexto, String nome) {
        Element element = primeiro(contexto, nome);
        return element == null ? null : element.getTextContent().trim();
    }

    private String textoDireto(Element contexto, String nome) {
        for (Node node = contexto.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && nome.equals(element.getLocalName())) {
                return element.getTextContent().trim();
            }
        }
        return null;
    }

    private int inteiro(Node contexto, String nome, int padrao) {
        String value = texto(contexto, nome);
        return value == null || value.isBlank() ? padrao : Integer.parseInt(value);
    }

    private int inteiroDireto(Element contexto, String nome, int padrao) {
        String value = textoDireto(contexto, nome);
        return value == null || value.isBlank() ? padrao : Integer.parseInt(value);
    }

    private Integer inteiroOpcional(Node contexto, String nome) {
        String value = texto(contexto, nome);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private String primeiroNaoVazio(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
