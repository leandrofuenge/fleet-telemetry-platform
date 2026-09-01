package com.telemetria.integration.nfe.soap;

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

import com.telemetria.integration.nfe.domain.exception.NfeSefazUnavailableException;

/**
 * Valida respostas SOAP recebidas da SEFAZ NF-e.
 *
 * Responsabilidades:
 * - validar se a resposta é um XML bem formado;
 * - proteger o parser contra XXE e recursos externos;
 * - validar o Envelope e a existência de um único SOAP 1.2 Body;
 * - identificar SOAP Fault;
 * - transformar SOAP Fault em exceção de infraestrutura.
 *
 * Esta classe não interpreta o conteúdo específico da NF-e.
 */
@Component
public class NfeSoapResponseValidator {

    private static final String SOAP_NAMESPACE =
            "http://www.w3.org/2003/05/soap-envelope";
    private static final String SOAP_ENVELOPE = "Envelope";
    private static final String SOAP_FAULT = "Fault";
    private static final String SOAP_BODY = "Body";

    /**
     * Valida a resposta SOAP retornada pela SEFAZ.
     *
     * @param respostaSoap XML SOAP retornado pela SEFAZ
     * @param service operação SOAP executada
     */
    public void validar(
            String respostaSoap,
            NfeSoapService service) {

        validarParametros(respostaSoap, service);

        try {

            Document document = criarDocumentBuilder()
                    .parse(
                            new ByteArrayInputStream(
                                    respostaSoap.getBytes(
                                            StandardCharsets.UTF_8)));

            Element body = obterSoapBody(document);

            validarFault(body, service);
            validarConteudo(body, service);

        } catch (NfeSefazUnavailableException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new NfeSefazUnavailableException(
                    "Resposta SOAP inválida recebida da SEFAZ NF-e para a operação "
                            + service.soapAction()
                            + ".",
                    exception);
        }
    }

    /**
     * Cria uma fábrica XML configurada para impedir
     * ataques XXE e acesso a recursos externos.
     */
    private DocumentBuilderFactory criarFactorySegura() {

        try {

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true);

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false);

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false);

            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);

            factory.setXIncludeAware(false);

            factory.setExpandEntityReferences(false);

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    "");

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    "");

            return factory;

        } catch (ParserConfigurationException exception) {

            throw new IllegalStateException(
                    "Não foi possível configurar o parser XML seguro para a NF-e.",
                    exception);
        }
    }

    /**
     * Cria um DocumentBuilder a partir da fábrica segura.
     *
     * DocumentBuilder não deve ser compartilhado entre threads.
     */
    private DocumentBuilder criarDocumentBuilder()
            throws ParserConfigurationException {

        return criarFactorySegura().newDocumentBuilder();
    }

    /**
     * Obtém exatamente um SOAP 1.2 Body diretamente dentro do Envelope.
     */
    private Element obterSoapBody(Document document) {

        Element envelope = document.getDocumentElement();
        if (envelope == null
                || !SOAP_NAMESPACE.equals(envelope.getNamespaceURI())
                || !SOAP_ENVELOPE.equals(envelope.getLocalName())) {

            throw new NfeSefazUnavailableException(
                    "Resposta da SEFAZ não possui um Envelope SOAP 1.2 válido.");
        }

        Element body = null;
        int quantidadeBodies = 0;
        for (Node node = envelope.getFirstChild();
                node != null;
                node = node.getNextSibling()) {

            if (node instanceof Element element
                    && SOAP_NAMESPACE.equals(element.getNamespaceURI())
                    && SOAP_BODY.equals(element.getLocalName())) {
                body = element;
                quantidadeBodies++;
            }
        }

        if (quantidadeBodies != 1) {

            throw new NfeSefazUnavailableException(
                    "Resposta SOAP NF-e deve possuir exatamente um Body SOAP 1.2.");
        }

        return body;
    }

    /**
     * Verifica se o SOAP Body contém um Fault.
     */
    private void validarFault(
            Element body,
            NfeSoapService service) {

        Element fault = filhoDireto(body, SOAP_NAMESPACE, SOAP_FAULT);
        if (fault == null) {
            return;
        }

        String mensagem = obterMensagemFault(fault);

        throw new NfeSefazUnavailableException(
                "SEFAZ retornou SOAP Fault para a operação "
                        + service.soapAction()
                        + ": "
                        + mensagem);
    }

    /**
     * Extrai a mensagem do SOAP Fault.
     *
     * Usa a estrutura SOAP 1.2 Fault/Reason/Text.
     */
    private String obterMensagemFault(Element fault) {

        Element reason = filhoDireto(fault, SOAP_NAMESPACE, "Reason");
        String mensagem = reason == null
                ? null
                : texto(reason, SOAP_NAMESPACE, "Text", null);

        if (mensagem != null && !mensagem.isBlank()) {
            return limitarMensagem(mensagem);
        }

        return "sem descrição";
    }

    private void validarConteudo(
            Element body,
            NfeSoapService service) {

        Element resposta = primeiroFilhoElemento(body);
        if (resposta == null
                || !service.namespace().equals(resposta.getNamespaceURI())
                || !service.elementoResposta().equals(resposta.getLocalName())) {

            throw new NfeSefazUnavailableException(
                    "Resposta SOAP inválida para a operação "
                            + service.soapAction()
                            + ": elemento esperado {"
                            + service.namespace()
                            + "}"
                            + service.elementoResposta()
                            + ".");
        }

        if (service.elementoResultado() != null
                && filhoDireto(
                        resposta,
                        service.namespace(),
                        service.elementoResultado()) == null) {

            throw new NfeSefazUnavailableException(
                    "Resposta SOAP da operação "
                            + service.soapAction()
                            + " não contém o resultado esperado "
                            + service.elementoResultado()
                            + ".");
        }
    }

    /**
     * Obtém o texto do primeiro elemento encontrado
     * com o namespace informado.
     */
    private String texto(
            Element contexto,
            String namespace,
            String nome,
            String padrao) {

        NodeList nodes =
                contexto.getElementsByTagNameNS(namespace, nome);

        if (nodes.getLength() == 0) {
            return padrao;
        }

        Node node = nodes.item(0);

        if (node == null) {
            return padrao;
        }

        String texto = node.getTextContent();

        if (texto == null) {
            return padrao;
        }

        texto = texto.trim();

        return texto.isEmpty()
                ? padrao
                : texto;
    }

    private Element filhoDireto(
            Element parent,
            String namespace,
            String localName) {

        for (Node node = parent.getFirstChild();
                node != null;
                node = node.getNextSibling()) {

            if (node instanceof Element element
                    && namespace.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) {
                return element;
            }
        }

        return null;
    }

    private Element primeiroFilhoElemento(Element parent) {

        for (Node node = parent.getFirstChild();
                node != null;
                node = node.getNextSibling()) {

            if (node instanceof Element element) {
                return element;
            }
        }

        return null;
    }

    /**
     * Evita que uma mensagem de erro extremamente grande
     * seja propagada para logs ou exceções.
     */
    private String limitarMensagem(String mensagem) {

        final int limite = 500;

        if (mensagem.length() <= limite) {
            return mensagem;
        }

        return mensagem.substring(0, limite)
                + "...";
    }

    private void validarParametros(
            String respostaSoap,
            NfeSoapService service) {

        if (respostaSoap == null
                || respostaSoap.isBlank()) {

            throw new NfeSefazUnavailableException(
                    "SEFAZ não retornou uma resposta SOAP válida.");
        }

        if (service == null) {

            throw new IllegalArgumentException(
                    "Serviço SOAP da NF-e não pode ser nulo.");
        }
    }
}
