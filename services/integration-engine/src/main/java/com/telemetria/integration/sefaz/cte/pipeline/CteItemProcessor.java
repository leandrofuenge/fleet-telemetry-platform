package com.telemetria.integration.sefaz.cte.pipeline;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.telemetria.integration.sefaz.cte.domain.CteContext;
import com.telemetria.integration.sefaz.cte.domain.CteMetadata;
import com.telemetria.integration.sefaz.cte.domain.CteStatus;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.util.HashUtils;

/**
 * Processador individual para cada CT-e fracionado pelo Splitter de lote.
 *
 * Responsabilidades:
 *
 * 1. Validar o conteúdo recebido pelo Splitter.
 * 2. Normalizar o XML.
 * 3. Fazer parsing seguro do XML.
 * 4. Validar elemento raiz e namespace.
 * 5. Extrair metadados básicos do CT-e.
 * 6. Armazenar informações úteis no Exchange.
 *
 * NÃO é responsabilidade desta classe:
 *
 * - Validar XSD;
 * - Validar regras fiscais;
 * - Assinar digitalmente;
 * - Enviar para a SEFAZ;
 * - Interpretar resposta da SEFAZ.
 */
@Component("cteItemProcessor")
public class CteItemProcessor implements Processor {

    private static final Logger log =
            LoggerFactory.getLogger(CteItemProcessor.class);

    private static final String CTE_NAMESPACE =
            "http://www.portalfiscal.inf.br/cte";

    private static final String ROOT_ELEMENT = "CTe";

    private static final int MAX_XML_SIZE_BYTES = 5 * 1024 * 1024;

    @Override
    public void process(Exchange exchange) throws Exception {

        long inicio = System.nanoTime();

        try {

            log.debug(
                    "Iniciando processamento do item CT-e. ExchangeId={}",
                    exchange.getExchangeId()
            );

            /*
             * ==========================================================
             * 1. Obter conteúdo
             * ==========================================================
             */

            String xmlItem = exchange.getMessage().getBody(String.class);

            validarConteudo(xmlItem);

            /*
             * ==========================================================
             * 2. Normalização
             * ==========================================================
             */

            String xmlProcessado = normalizarXml(xmlItem);

            validarTamanho(xmlProcessado);

            /*
             * ==========================================================
             * 3. Parsing seguro
             * ==========================================================
             */

            Document document = parseXmlSeguro(xmlProcessado);

            /*
             * ==========================================================
             * 4. Validar estrutura básica do CT-e
             * ==========================================================
             */

            Element root = validarEstruturaCte(document);

            /*
             * ==========================================================
             * 5. Extrair metadados
             * ==========================================================
             */

            CteMetadata metadata =
                    extrairMetadata(document, root);

            
            CteContext context =
                    new CteContext(
                            null,
                            metadata,
                            xmlItem,
                            xmlProcessado,
                            null,
                            HashUtils.sha256(xmlProcessado),
                            CteStatus.RECEBIDO,
                            0
                    );

            exchange.setProperty(
                    CteExchangeProperties.CTE_CONTEXT,
                    context
            );
            
            
            
            /*
             * ==========================================================
             * 6. Guardar informações no Exchange
             * ==========================================================
             */

            exchange.setProperty(
                    CteExchangeProperties.CTE_METADATA,
                    metadata
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_XML_NORMALIZADO,
                    xmlProcessado
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_CHAVE,
                    metadata.chave()
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_NUMERO,
                    metadata.numero()
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_SERIE,
                    metadata.serie()
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_MODELO,
                    metadata.modelo()
            );

            /*
             * ==========================================================
             * 7. Atualizar body
             * ==========================================================
             */

            exchange.getMessage().setBody(xmlProcessado);

            /*
             * ==========================================================
             * 8. Log
             * ==========================================================
             */

            long tempoMs =
                    (System.nanoTime() - inicio) / 1_000_000;

            log.info(
                    "CT-e preparado com sucesso. " +
                    "chave={}, numero={}, serie={}, modelo={}, " +
                    "tamanhoBytes={}, tempoMs={}, exchangeId={}",
                    metadata.chave(),
                    metadata.numero(),
                    metadata.serie(),
                    metadata.modelo(),
                    xmlProcessado.getBytes(StandardCharsets.UTF_8).length,
                    tempoMs,
                    exchange.getExchangeId()
            );

        } catch (CteException e) {

            registrarErro(exchange, inicio, e);

            throw e;

        } catch (Exception e) {

            registrarErro(exchange, inicio, e);

            throw new CteException(
                    "Erro inesperado durante o processamento inicial do CT-e.",
                    e
            );
        }
    }

    /**
     * Valida se o conteúdo recebido é utilizável.
     */
    private void validarConteudo(String xml) {

        if (xml == null) {
            throw new CteException(
                    "O item do lote CT-e está nulo."
            );
        }

        if (xml.isBlank()) {
            throw new CteException(
                    "O item do lote CT-e está vazio."
            );
        }
    }

    /**
     * Normaliza o XML sem tentar alterar arbitrariamente seu encoding.
     *
     * Como o body já é String, a conversão bytes -> String
     * normalmente ocorreu antes desta etapa.
     */
    private String normalizarXml(String xml) {

        String resultado = xml
                .replace("\uFEFF", "")
                .trim();

        if (resultado.isBlank()) {
            throw new CteException(
                    "O XML do CT-e ficou vazio após a normalização."
            );
        }

        if (!resultado.startsWith("<")) {
            throw new CteException(
                    "O conteúdo recebido não aparenta ser um XML."
            );
        }

        return resultado;
    }

    /**
     * Evita processar documentos absurdamente grandes.
     */
    private void validarTamanho(String xml) {

        int tamanho =
                xml.getBytes(StandardCharsets.UTF_8).length;

        if (tamanho > MAX_XML_SIZE_BYTES) {

            throw new CteException(
                    String.format(
                            "XML do CT-e excede o tamanho máximo permitido. " +
                            "Tamanho=%d bytes, máximo=%d bytes.",
                            tamanho,
                            MAX_XML_SIZE_BYTES
                    )
            );
        }
    }

    /**
     * Faz parsing seguro contra XXE e acesso a recursos externos.
     */
    private Document parseXmlSeguro(String xml) {

        try {

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            /*
             * Segurança XML / XXE
             */

            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true
            );

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

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    ""
            );

            factory.setXIncludeAware(false);

            factory.setExpandEntityReferences(false);

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            return builder.parse(
                    new InputSource(
                            new StringReader(xml)
                    )
            );

        } catch (Exception e) {

            throw new CteException(
                    "O XML do CT-e está malformado ou não pôde ser interpretado.",
                    e
            );
        }
    }

    /**
     * Valida o elemento raiz e namespace do documento.
     */
    private Element validarEstruturaCte(Document document) {

        Element root = document.getDocumentElement();

        if (root == null) {
            throw new CteException(
                    "O XML do CT-e não possui elemento raiz."
            );
        }

        String localName = root.getLocalName();
        String namespace = root.getNamespaceURI();

        if (!ROOT_ELEMENT.equals(localName)) {

            throw new CteException(
                    String.format(
                            "Elemento raiz inválido. " +
                            "Esperado=%s, encontrado=%s.",
                            ROOT_ELEMENT,
                            localName
                    )
            );
        }

        if (!CTE_NAMESPACE.equals(namespace)) {

            throw new CteException(
                    String.format(
                            "Namespace inválido para CT-e. " +
                            "Esperado=%s, encontrado=%s.",
                            CTE_NAMESPACE,
                            namespace
                    )
            );
        }

        return root;
    }

    /**
     * Extrai somente informações básicas.
     *
     * A validação definitiva dos campos deve ser feita pelo XSD
     * e pelas regras de negócio.
     */
    private CteMetadata extrairMetadata(
            Document document,
            Element root
    ) {

        String chave = null;
        String numero = null;
        String serie = null;
        String modelo = null;
        String versao = null;

        /*
         * Versão do leiaute.
         *
         * Normalmente encontrada em infCte/@versao.
         */
        Element infCte =
                primeiroElementoPorTag(document, "infCte");

        if (infCte != null) {

            versao = infCte.getAttribute("versao");

            String id = infCte.getAttribute("Id");

            if (id != null && !id.isBlank()) {

                chave = normalizarChave(id);
            }
        }

        /*
         * Ide
         */

        Element ide =
                primeiroElementoPorTag(document, "ide");

        if (ide != null) {

            numero = textoFilho(ide, "nCT");

            serie = textoFilho(ide, "serie");

            modelo = textoFilho(ide, "mod");
        }

        return new CteMetadata(
                chave,
                numero,
                serie,
                modelo,
                versao
        );
    }

    /**
     * Localiza o primeiro elemento pelo localName.
     */
    private Element primeiroElementoPorTag(
            Document document,
            String localName
    ) {

        var nodes =
                document.getElementsByTagNameNS(
                        CTE_NAMESPACE,
                        localName
                );

        if (nodes.getLength() == 0) {
            return null;
        }

        return (Element) nodes.item(0);
    }

    /**
     * Obtém texto de um filho direto pelo namespace.
     */
    private String textoFilho(
            Element parent,
            String localName
    ) {

        var nodes =
                parent.getElementsByTagNameNS(
                        CTE_NAMESPACE,
                        localName
                );

        if (nodes.getLength() == 0) {
            return null;
        }

        String valor =
                nodes.item(0).getTextContent();

        if (valor == null) {
            return null;
        }

        valor = valor.trim();

        return valor.isBlank() ? null : valor;
    }

    /**
     * Remove o prefixo esperado de uma chave de CT-e,
     * caso o atributo Id venha como "CTe123...".
     */
    private String normalizarChave(String id) {

        if (id == null) {
            return null;
        }

        String resultado = id.trim();

        if (resultado.startsWith("CTe")) {
            resultado = resultado.substring(3);
        }

        return resultado.isBlank()
                ? null
                : resultado;
    }

    private void registrarErro(
            Exchange exchange,
            long inicio,
            Exception exception
    ) {

        long tempoMs =
                (System.nanoTime() - inicio) / 1_000_000;

        String chave =
                exchange.getProperty(
                        CteExchangeProperties.CTE_CHAVE,
                        String.class
                );

        log.error(
                "Falha no processamento inicial do CT-e. " +
                "chave={}, tempoMs={}, exchangeId={}, " +
                "exception={}",
                chave,
                tempoMs,
                exchange.getExchangeId(),
                exception.getClass().getSimpleName(),
                exception
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_PROCESSAMENTO_FALHOU,
                true
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_ERRO,
                exception.getMessage()
        );
    }
}
