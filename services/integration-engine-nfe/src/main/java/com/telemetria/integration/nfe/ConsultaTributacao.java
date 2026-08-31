package com.telemetria.integration.nfe;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.util.ConfiguracoesUtil;
import com.telemetria.integration.nfe.util.ObjetoUtil;
import com.telemetria.integration.nfe.util.UtilitarioServicoWeb;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;

/**
 * Classe responsável por consultar classificações tributárias para a Reforma Tributária, utiliza o serviço web disponibilizado pelo governo federal.
 * Foi criada para funcionar da forma mais genérica possível, permitindo a consulta e manipulação dos dados retornados em JSON se necessário.
 * <p>
 * Oferece quatro modos de uso:
 * <p>1. Retorno direto do JSON string (método getJson)</p>
 * <p>2. Conversão genérica para qualquer tipo de objeto (método get com Class)</p>
 * <p>3. Conversão genérica com TypeReference para tipos complexos (método get com TypeReference)</p>
 * <p>4. Validação de estrutura JSON vs DTO (método validate)</p>
 * <p>
 * Exemplos de uso:
 * <pre>
 * // Obter JSON bruto
 * String json = ConsultaTributacao.getJson(config);
 *
 * // Converter para List de DTOs
 * List&lt;CstDTO&gt; lista = ConsultaTributacao.get(config, new TypeReference&lt;List&lt;CstDTO&gt;&gt;() {});
 *
 * // Com filtros
 * Map&lt;String, String&gt; params = new HashMap&lt;&gt;();
 * params.put("Cst", "00");
 * List&lt;CstDTO&gt; filtrado = ConsultaTributacao.get(config, params, new TypeReference&lt;List&lt;CstDTO&gt;&gt;() {});
 *
 * // Validar estrutura
 * ValidationReport report = ConsultaTributacao.validate(config, CstDTO.class);
 * </pre>
 *
 */
public class ConsultaTributacao {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(ConsultaTributacao.class.getName());

    private ConsultaTributacao() {
    }

    private static final String SECTION = "CFF";
    private static final String KEY = "classTrib";

    private static final ObjectMapper MAPPER = createObjectMapper();
    private static final ObjectMapper STRICT_MAPPER = createStrictObjectMapper();

    /**
     * Cria o ObjectMapper utilizado pela consulta.
     *
     * A instância é criada uma única vez e reutilizada,
     * evitando custo de configuração a cada requisição.
     */
    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    /**
     * Cria o ObjectMapper utilizado para validação rigorosa do JSON.
     *
     * A instância é criada uma única vez e reutilizada nas operações
     * de desserialização.
     */
    private static ObjectMapper createStrictObjectMapper() {
        return new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
    }

    /**
     * Executa requisição HTTP GET sem parâmetros de query
     * e retorna o JSON bruto da resposta.
     *
     * @param config Configurações da NFe contendo certificado digital
     * @return String contendo o JSON de resposta
     * @throws ExcecaoNfe Se houver erro de configuração, certificado ou SSL
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static String getJson(ConfiguracoesNfe config)
            throws ExcecaoNfe, IOException {

        return getJson(config, null);
    }

    /**
     * Executa requisição HTTP GET com parâmetros de query e retorna o JSON bruto.
     *
     * @param config      Configurações da NFe contendo certificado digital
     * @param queryParams Parâmetros de query
     * @return JSON da resposta
     * @throws ExcecaoNfe Se houver erro de configuração, certificado ou SSL
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static String getJson(
            ConfiguracoesNfe config,
            Map<String, String> queryParams
    ) throws ExcecaoNfe, IOException {

        ConfiguracoesUtil.iniciaConfiguracoes(config);

        Certificado certificado = config.getCertificado();
        if (certificado == null) {
            throw new ExcecaoNfe("Certificado digital não configurado");
        }

        String urlBase = UtilitarioServicoWeb.getCustomUrl(config, SECTION, KEY);
        String url = buildUrlWithParams(urlBase, queryParams);

        try {
            HttpClient httpClient = createHttpClient(config, certificado, url);

            if (httpClient != null) {
                return executeRequestWithHttpClient(httpClient, url, certificado);
            }
        } catch (Exception e) {
            log.warning(
                "[ConsultaTributacao] Falha ao criar HttpClient, " +
                "tentando fallback: " + e.getMessage()
            );
        }

        return executeFallback(config, url);
    }

    private static String executeFallback(
            ConfiguracoesNfe config,
            String url
    ) throws ExcecaoNfe, IOException {

        SSLSocketFactory sslFactory = tryResolveSslSocketFactory(config);

        if (sslFactory == null) {
            throw new ExcecaoNfe(
                "Não foi possível configurar SSL/TLS para a requisição."
            );
        }

        return executeRequestWithSslFactory(sslFactory, url);
    }

    /**
     * Executa requisição HTTP GET e converte a resposta JSON
     * para o tipo especificado.
     *
     * @param <T>    Tipo do objeto de retorno
     * @param config Configurações da NFe contendo certificado digital
     * @param clazz  Classe do objeto de destino
     * @return Objeto do tipo T com os dados desserializados
     * @throws ExcecaoNfe Se houver erro de configuração, certificado ou conversão
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static <T> T get(
            ConfiguracoesNfe config,
            Class<T> clazz
    ) throws ExcecaoNfe, IOException {

        Objects.requireNonNull(clazz, "A classe de destino não pode ser nula");

        String json = getJson(config);

        return convertJsonToObject(json, clazz);
    }

    /**
     * Executa requisição HTTP GET com parâmetros e converte o JSON
     * para o tipo especificado.
     *
     * @param <T>         Tipo do objeto de retorno
     * @param config      Configurações da NFe
     * @param queryParams Parâmetros de query
     * @param clazz       Classe do objeto de destino
     * @return Objeto do tipo T desserializado
     * @throws ExcecaoNfe Se houver erro de configuração, certificado ou conversão
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static <T> T get(
            ConfiguracoesNfe config,
            Map<String, String> queryParams,
            Class<T> clazz
    ) throws ExcecaoNfe, IOException {

        Objects.requireNonNull(clazz, "A classe de destino não pode ser nula");

        String json = getJson(config, queryParams);

        return convertJsonToObject(json, clazz);
    }

    /**
     * Executa requisição HTTP GET e converte o JSON para um tipo complexo.
     *
     * <p>Utiliza {@link TypeReference} do Jackson para preservar
     * informações de tipo genérico, como {@code List<MeuDto>} ou
     * {@code Map<String, MeuDto>}.</p>
     *
     * @param <T>     Tipo do objeto de retorno
     * @param config  Configurações da NFe
     * @param typeRef Referência do tipo genérico desejado
     * @return Objeto do tipo T desserializado
     * @throws ExcecaoNfe Se houver erro de configuração, certificado ou conversão
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static <T> T get(
            ConfiguracoesNfe config,
            TypeReference<T> typeRef
    ) throws ExcecaoNfe, IOException {

        Objects.requireNonNull(
                typeRef,
                "A referência do tipo não pode ser nula"
        );

        String json = getJson(config);

        return convertJsonToObject(json, typeRef);
    }

    /**
     * Executa requisição HTTP GET com parâmetros e converte a resposta
     * para um tipo complexo.
     *
     * <p>Utiliza {@link TypeReference} para preservar informações de
     * tipo genérico durante a desserialização.</p>
     *
     * @param <T>         Tipo do objeto de retorno
     * @param config      Configurações da NFe
     * @param queryParams Parâmetros de query
     * @param typeRef     Referência do tipo genérico desejado
     * @return Objeto do tipo T desserializado
     * @throws ExcecaoNfe Se houver erro de configuração, certificado ou conversão
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static <T> T get(
            ConfiguracoesNfe config,
            Map<String, String> queryParams,
            TypeReference<T> typeRef
    ) throws ExcecaoNfe, IOException {

        Objects.requireNonNull(
                typeRef,
                "A referência do tipo não pode ser nula"
        );

        String json = getJson(config, queryParams);

        return convertJsonToObject(json, typeRef);
    }

    /**
     * Valida a estrutura do JSON retornado pela API contra um DTO.
     * Retorna um relatório detalhado de inconsistências.
     *
     * @param config Configurações da NFe
     * @param clazz  Classe do DTO a ser validada
     * @return ValidationReport com detalhes das inconsistências
     * @throws ExcecaoNfe Se houver erro na consulta ou validação
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static ValidationReport validate(
            ConfiguracoesNfe config,
            Class<?> clazz
    ) throws ExcecaoNfe, IOException {

        Objects.requireNonNull(
                clazz,
                "A classe do DTO não pode ser nula"
        );

        String json = getJson(config);

        return validateJsonStructure(json, clazz);
    }

    /**
     * Valida a estrutura do JSON retornado pela API contra um DTO,
     * utilizando parâmetros de query.
     *
     * @param config      Configurações da NFe
     * @param queryParams Parâmetros de query
     * @param clazz       Classe do DTO a ser validada
     * @return ValidationReport com detalhes das inconsistências
     * @throws ExcecaoNfe Se houver erro na consulta ou validação
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static ValidationReport validate(
            ConfiguracoesNfe config,
            Map<String, String> queryParams,
            Class<?> clazz
    ) throws ExcecaoNfe, IOException {

        Objects.requireNonNull(
                clazz,
                "A classe do DTO não pode ser nula"
        );

        String json = getJson(config, queryParams);

        return validateJsonStructure(json, clazz);
    }

    /**
     * Valida a estrutura do JSON retornado pela API contra a classe interna
     * informada.
     *
     * <p>O {@link TypeReference} representa o tipo externo esperado,
     * como {@code List<CstDTO>} ou {@code Map<String, CstDTO>}, enquanto
     * a validação estrutural é realizada sobre {@code innerClass}.</p>
     *
     * @param config     Configurações da NFe
     * @param typeRef    TypeReference do tipo externo esperado
     * @param innerClass Classe interna utilizada como referência estrutural
     * @return ValidationReport com detalhes das inconsistências
     * @throws ExcecaoNfe Se houver erro na consulta ou validação
     * @throws IOException Se houver erro de comunicação HTTP
     */
    public static ValidationReport validate(
            ConfiguracoesNfe config,
            TypeReference<?> typeRef,
            Class<?> innerClass
    ) throws ExcecaoNfe, IOException {

        Objects.requireNonNull(
                typeRef,
                "A referência do tipo não pode ser nula"
        );

        Objects.requireNonNull(
                innerClass,
                "A classe interna não pode ser nula"
        );

        String json = getJson(config);

        return validateJsonStructure(json, innerClass);
    }

    /**
     * Testa se o JSON retornado pela API pode ser convertido para o DTO
     * utilizando as regras de desserialização rigorosa.
     *
     * @param config Configurações da NFe
     * @param clazz  Classe do DTO
     * @return true se o JSON for compatível com o DTO; false caso contrário
     */
    public static boolean isCompatible(
            ConfiguracoesNfe config,
            Class<?> clazz
    ) {

        Objects.requireNonNull(
                clazz,
                "A classe do DTO não pode ser nula"
        );

        try {
            String json = getJson(config);

            STRICT_MAPPER.readValue(json, clazz);

            return true;

        } catch (JsonProcessingException e) {
            log.warning(
                    "[ConsultaTributacao] JSON incompatível com "
                    + clazz.getSimpleName()
                    + ": "
                    + e.getOriginalMessage()
            );

            return false;

        } catch (IOException e) {
            log.warning(
                    "[ConsultaTributacao] Erro de comunicação ao verificar "
                    + "compatibilidade: "
                    + e.getMessage()
            );

            return false;

        } catch (ExcecaoNfe e) {
            log.warning(
                    "[ConsultaTributacao] Erro de configuração ao verificar "
                    + "compatibilidade: "
                    + e.getMessage()
            );

            return false;
        }
    }

    /**
     * Valida a estrutura do JSON contra uma classe DTO.
     *
     * <p>Identifica campos extras, campos faltantes, incompatibilidades
     * de tipo e tenta realizar a conversão estrita do JSON.</p>
     *
     * @param json  JSON a ser validado
     * @param clazz Classe do DTO esperado
     * @return Relatório contendo as inconsistências encontradas
     * @throws ExcecaoNfe Se o JSON não puder ser analisado
     */
    private static ValidationReport validateJsonStructure(
            String json,
            Class<?> clazz
    ) throws ExcecaoNfe {

        Objects.requireNonNull(json, "O JSON não pode ser nulo");
        Objects.requireNonNull(clazz, "A classe do DTO não pode ser nula");

        ValidationReport report = new ValidationReport();

        try {
            JsonNode rootNode = MAPPER.readTree(json);

            if (rootNode == null || rootNode.isNull()) {
                throw new ExcecaoNfe("JSON vazio ou nulo");
            }

            JsonNode nodeToValidate = rootNode;

            // Se o JSON for um array, valida o primeiro elemento.
            if (rootNode.isArray()) {
                report.setArrayDetected(true);
                report.setArraySize(rootNode.size());

                if (rootNode.isEmpty()) {
                    report.setStrictConversionSuccess(true);
                    return report;
                }

                nodeToValidate = rootNode.get(0);
            }

            if (!nodeToValidate.isObject()) {
                report.setStrictConversionSuccess(false);
                report.setStrictConversionError(
                        "A estrutura esperada é um objeto JSON, mas foi encontrado: "
                                + getJsonNodeType(nodeToValidate)
                );
                return report;
            }

            Map<String, Class<?>> expectedFields = extractDtoFields(clazz);

            Set<String> jsonFields = new HashSet<>();
            nodeToValidate.fieldNames().forEachRemaining(jsonFields::add);

            Set<String> extraFields = new HashSet<>(jsonFields);
            extraFields.removeAll(expectedFields.keySet());
            report.setExtraFields(extraFields);

            Set<String> missingFields = new HashSet<>(expectedFields.keySet());
            missingFields.removeAll(jsonFields);
            report.setMissingFields(missingFields);

            Map<String, String> typeErrors = new HashMap<>();

            for (Map.Entry<String, Class<?>> entry : expectedFields.entrySet()) {

                String fieldName = entry.getKey();

                if (!jsonFields.contains(fieldName)) {
                    continue;
                }

                JsonNode fieldNode = nodeToValidate.get(fieldName);
                Class<?> expectedType = entry.getValue();

                if (!isTypeCompatible(fieldNode, expectedType)) {
                    typeErrors.put(
                            fieldName,
                            "Esperado: " + expectedType.getSimpleName()
                                    + ", Encontrado: "
                                    + getJsonNodeType(fieldNode)
                    );
                }
            }

            report.setTypeErrors(typeErrors);

            // Mantém a conversão rigorosa para detectar problemas
            // que não são identificados pela comparação estrutural.
            try {
                STRICT_MAPPER.readValue(json, clazz);
                report.setStrictConversionSuccess(true);

            } catch (JsonProcessingException e) {
                report.setStrictConversionSuccess(false);
                report.setStrictConversionError(e.getOriginalMessage());
            }

        } catch (JsonProcessingException e) {
            throw new ExcecaoNfe(
                    "JSON inválido: " + e.getOriginalMessage(),
                    e
            );
        } catch (IOException e) {
            throw new ExcecaoNfe(
                    "Erro ao ler JSON: " + e.getMessage(),
                    e
            );
        }

        return report;
    }

    private static final ConcurrentMap<Class<?>, Map<String, Class<?>>> DTO_FIELDS_CACHE =
            new ConcurrentHashMap<>();

    /**
     * Extrai os campos do DTO considerando as anotações {@link JsonProperty}.
     *
     * <p>A estrutura de cada classe é armazenada em cache para evitar
     * chamadas repetidas de Reflection durante as validações.</p>
     *
     * @param clazz Classe do DTO
     * @return Mapa contendo nome do campo JSON e seu tipo Java
     */
    private static Map<String, Class<?>> extractDtoFields(Class<?> clazz) {

        return DTO_FIELDS_CACHE.computeIfAbsent(clazz, ConsultaTributacao::buildDtoFields);
    }

    /**
     * Constrói a estrutura de campos do DTO utilizando Reflection.
     */
    private static Map<String, Class<?>> buildDtoFields(Class<?> clazz) {

        Map<String, Class<?>> fields = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {

            int modifiers = field.getModifiers();

            // Ignora campos static.
            if (Modifier.isStatic(modifiers)) {
                continue;
            }

            JsonProperty annotation = field.getAnnotation(JsonProperty.class);

            String jsonFieldName = annotation != null
                    && !annotation.value().isEmpty()
                    ? annotation.value()
                    : field.getName();

            fields.put(jsonFieldName, field.getType());
        }

        return Collections.unmodifiableMap(fields);
    }

    /**
     * Verifica se o tipo do JsonNode é compatível com o tipo Java esperado.
     *
     * @param node         Nó JSON
     * @param expectedType Tipo Java esperado
     * @return true se o tipo for compatível; false caso contrário
     */
    private static boolean isTypeCompatible(
            JsonNode node,
            Class<?> expectedType
    ) {

        if (node == null || node.isNull()) {
            return true;
        }

        // Strings
        if (expectedType == String.class
                || expectedType == Character.class
                || expectedType == char.class) {
            return node.isTextual();
        }

        // Tipos inteiros
        if (expectedType == Integer.class
                || expectedType == int.class
                || expectedType == Long.class
                || expectedType == long.class
                || expectedType == Short.class
                || expectedType == short.class
                || expectedType == Byte.class
                || expectedType == byte.class) {
            return node.isIntegralNumber();
        }

        // Tipos decimais
        if (expectedType == Double.class
                || expectedType == double.class
                || expectedType == Float.class
                || expectedType == float.class
                || expectedType == java.math.BigDecimal.class) {
            return node.isNumber();
        }

        // Boolean
        if (expectedType == Boolean.class
                || expectedType == boolean.class) {
            return node.isBoolean();
        }

        // Arrays e coleções
        if (expectedType.isArray()
                || Collection.class.isAssignableFrom(expectedType)) {
            return node.isArray();
        }

        // Mapas
        if (Map.class.isAssignableFrom(expectedType)) {
            return node.isObject();
        }

        // Enum
        if (expectedType.isEnum()) {
            return node.isTextual();
        }

        // Tipos complexos
        return node.isObject();
    }

    /**
     * Retorna uma descrição simplificada do tipo do {@link JsonNode}.
     *
     * @param node Nó JSON
     * @return Nome descritivo do tipo do nó
     */
    private static String getJsonNodeType(JsonNode node) {

        if (node == null || node.isNull()) {
            return "null";
        }

        if (node.isTextual()) {
            return "String";
        }

        if (node.isIntegralNumber()) {
            return "Integer";
        }

        if (node.isFloatingPointNumber() || node.isBigDecimal()) {
            return "Decimal";
        }

        if (node.isBoolean()) {
            return "Boolean";
        }

        if (node.isArray()) {
            return "Array";
        }

        if (node.isObject()) {
            return "Object";
        }

        return node.getNodeType().name();
    }

    /**
     * Converte um JSON para o tipo especificado.
     *
     * @param <T>   Tipo do objeto de destino
     * @param json  JSON a ser convertido
     * @param clazz Classe do objeto de destino
     * @return Objeto desserializado
     * @throws ExcecaoNfe Se ocorrer erro durante a conversão
     */
    public static <T> T convertJsonToObject(
            String json,
            Class<T> clazz
    ) throws ExcecaoNfe {

        Objects.requireNonNull(json, "O JSON não pode ser nulo");
        Objects.requireNonNull(clazz, "A classe de destino não pode ser nula");

        try {
            log.info(
                    "[ConsultaTributacao] Convertendo JSON para "
                            + clazz.getSimpleName()
            );

            return MAPPER.readValue(json, clazz);

        } catch (JsonProcessingException e) {

            log.severe(
                    "[ConsultaTributacao] Erro ao converter JSON para "
                            + clazz.getSimpleName()
                            + ": "
                            + e.getOriginalMessage()
            );

            throw new ExcecaoNfe(
                    "Erro ao processar resposta JSON para "
                            + clazz.getSimpleName()
                            + ": "
                            + e.getOriginalMessage(),
                    e
            );

        } catch (IOException e) {

            log.severe(
                    "[ConsultaTributacao] Erro de I/O ao converter JSON para "
                            + clazz.getSimpleName()
                            + ": "
                            + e.getMessage()
            );

            throw new ExcecaoNfe(
                    "Erro de I/O ao processar resposta JSON: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Converte um JSON para um tipo complexo utilizando {@link TypeReference}.
     *
     * <p>O {@code TypeReference} preserva informações de tipos genéricos,
     * permitindo conversões como {@code List<DTO>} e {@code Map<String, DTO>}.</p>
     *
     * @param <T>     Tipo do objeto de destino
     * @param json    JSON a ser convertido
     * @param typeRef Referência do tipo de destino
     * @return Objeto desserializado
     * @throws ExcecaoNfe Se ocorrer erro durante a conversão
     */
    public static <T> T convertJsonToObject(
            String json,
            TypeReference<T> typeRef
    ) throws ExcecaoNfe {

        Objects.requireNonNull(
                json,
                "O JSON não pode ser nulo"
        );

        Objects.requireNonNull(
                typeRef,
                "A referência do tipo não pode ser nula"
        );

        try {
            log.info(
                    "[ConsultaTributacao] Convertendo JSON para tipo complexo"
            );

            return MAPPER.readValue(json, typeRef);

        } catch (JsonProcessingException e) {

            log.severe(
                    "[ConsultaTributacao] Erro ao converter JSON: "
                            + e.getOriginalMessage()
            );

            throw new ExcecaoNfe(
                    "Erro ao processar resposta JSON: "
                            + e.getOriginalMessage(),
                    e
            );

        } catch (IOException e) {

            log.severe(
                    "[ConsultaTributacao] Erro de I/O ao converter JSON: "
                            + e.getMessage()
            );

            throw new ExcecaoNfe(
                    "Erro de I/O ao processar resposta JSON: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Adiciona parâmetros de query à URL base.
     *
     * <p>Parâmetros com valor nulo ou vazio são ignorados.
     * Chaves e valores são codificados para utilização segura na URL.</p>
     *
     * @param baseUrl     URL base
     * @param queryParams Parâmetros de query
     * @return URL com os parâmetros adicionados
     */
    private static String buildUrlWithParams(
            String baseUrl,
            Map<String, String> queryParams
    ) {

        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        StringBuilder url = new StringBuilder(baseUrl);
        boolean first = !baseUrl.contains("?");

        for (Map.Entry<String, String> entry : queryParams.entrySet()) {

            String key = entry.getKey();
            String value = entry.getValue();

            if (key == null || value == null || value.trim().isEmpty()) {
                continue;
            }

            url.append(first ? '?' : '&');

            url.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            url.append('=');
            url.append(URLEncoder.encode(value, StandardCharsets.UTF_8));

            first = false;
        }

        return url.toString();
    }

    /**
     * Cria um HttpClient configurado para comunicação HTTPS utilizando
     * o certificado digital informado.
     *
     * @param config      Configurações da NFe
     * @param certificado Certificado digital
     * @param url         URL do serviço
     * @return HttpClient configurado
     * @throws Exception Se ocorrer erro na criação do cliente HTTPS
     */
    private static HttpClient createHttpClient(
            ConfiguracoesNfe config,
            Certificado certificado,
            String url
    ) throws Exception {

        InputStream cacert = config.getCacert();

        if (cacert != null) {
            return CertificadoService.getHttpsClient(
                    certificado,
                    url,
                    cacert
            );
        }

        return CertificadoService.getHttpsClient(
                certificado,
                url
        );
    }

    /**
     * Executa uma requisição HTTP GET e retorna o conteúdo JSON.
     *
     * <p>No modo multithreading, o host da requisição é ajustado com base
     * na URL informada, preservando o protocolo configurado no cliente.</p>
     *
     * @param httpClient  HttpClient configurado
     * @param url         URL da requisição
     * @param certificado Certificado digital
     * @return Conteúdo JSON retornado pela API
     * @throws IOException Se ocorrer erro de comunicação HTTP
     */
    private static String executeRequestWithHttpClient(
            HttpClient httpClient,
            String url,
            Certificado certificado
    ) throws IOException {

        String uri = prepareRequestUri(
                httpClient,
                url,
                certificado
        );

        GetMethod getMethod = new GetMethod(uri);

        try {
            getMethod.setRequestHeader(
                    "Accept",
                    "application/json"
            );

            int statusCode = httpClient.executeMethod(getMethod);

            log.info(
                    "[ConsultaTributacao] Status HTTP: "
                            + statusCode
            );

            InputStream responseStream =
                    getMethod.getResponseBodyAsStream();

            String responseBody = inputStreamToString(responseStream);

            if (statusCode != HttpStatus.SC_OK) {

                log.severe(
                        "[ConsultaTributacao] Erro HTTP "
                                + statusCode
                                + ": "
                                + responseBody
                );

                throw new IOException(
                        "Erro HTTP "
                                + statusCode
                                + ": "
                                + responseBody
                );
            }

            return responseBody;

        } finally {
            getMethod.releaseConnection();
        }
    }

    /**
     * Prepara a URI utilizada pelo GetMethod.
     */
    private static String prepareRequestUri(
            HttpClient httpClient,
            String url,
            Certificado certificado
    ) {

        if (!certificado.isModoMultithreading()) {
            return url;
        }

        HostConfiguration hostConfiguration =
                httpClient.getHostConfiguration();

        Protocol protocol =
                hostConfiguration.getProtocol();

        if (protocol == null) {
            return url;
        }

        try {
            URL parsedUrl = new URL(url);

            hostConfiguration.setHost(
                    parsedUrl.getHost(),
                    parsedUrl.getPort(),
                    protocol
            );

            return parsedUrl.getFile();

        } catch (MalformedURLException e) {

            log.warning(
                    "[ConsultaTributacao] URL inválida: "
                            + e.getMessage()
            );

            return url;
        }
    }

    /**
     * Executa uma requisição HTTP GET utilizando uma SSLSocketFactory.
     *
     * @param sslFactory SSLSocketFactory configurada
     * @param url        URL da requisição
     * @return Conteúdo JSON retornado pela API
     * @throws IOException Se ocorrer erro de comunicação HTTP
     */
    private static String executeRequestWithSslFactory(
            SSLSocketFactory sslFactory,
            String url
    ) throws IOException {

        URL parsedUrl = new URL(url);

        HttpsURLConnection connection =
                (HttpsURLConnection) parsedUrl.openConnection();

        try {
            connection.setSSLSocketFactory(sslFactory);
            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setDoInput(true);

            int statusCode = connection.getResponseCode();

            log.info(
                    "[ConsultaTributacao] Status HTTP: "
                            + statusCode
            );

            if (statusCode != HttpStatus.SC_OK) {

                String errorBody = readErrorStream(
                        connection.getErrorStream()
                );

                log.severe(
                        "[ConsultaTributacao] Erro HTTP "
                                + statusCode
                                + ": "
                                + errorBody
                );

                throw new IOException(
                        "Erro HTTP "
                                + statusCode
                                + ": "
                                + errorBody
                );
            }

            try (InputStream inputStream =
                         new BufferedInputStream(
                                 connection.getInputStream()
                         )) {

                return inputStreamToString(inputStream);
            }

        } finally {
            connection.disconnect();
        }
    }

    private static String readErrorStream(InputStream errorStream)
            throws IOException {

        return inputStreamToString(errorStream);
    }

    private static SSLSocketFactory tryResolveSslSocketFactory(ConfiguracoesNfe config) {
        Certificado certificado = config.getCertificado();

        if (certificado == null) {
            return null;
        }

        SSLSocketFactory sslFactory = resolveSslFactoryFromService(certificado);

        if (sslFactory != null) {
            return sslFactory;
        }

        return resolveSslFactoryFromCertificate(certificado);
    }

    private static SSLSocketFactory resolveSslFactoryFromService(Certificado certificado) {
        try {
            Class<?> serviceClass = Class.forName(
                    "br.com.swconsultoria.certificado.CertificadoService"
            );

            try {
                Method method = serviceClass.getMethod(
                        "getSSLSocketFactory",
                        Certificado.class
                );

                Object result = method.invoke(null, certificado);

                if (result instanceof SSLSocketFactory) {
                    return (SSLSocketFactory) result;
                }

            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method method = serviceClass.getMethod(
                        "getSslContext",
                        Certificado.class
                );

                Object result = method.invoke(null, certificado);

                if (result instanceof SSLContext) {
                    return ((SSLContext) result).getSocketFactory();
                }

            } catch (NoSuchMethodException ignored) {
            }

        } catch (ClassNotFoundException e) {
            log.fine(
                    "[ConsultaTributacao] CertificadoService não encontrado: "
                            + e.getMessage()
            );
        } catch (ReflectiveOperationException e) {
            log.fine(
                    "[ConsultaTributacao] Erro ao acessar CertificadoService: "
                            + e.getMessage()
            );
        }

        return null;
    }

    private static SSLSocketFactory resolveSslFactoryFromCertificate(
            Certificado certificado
    ) {
        Class<?> certificateClass = certificado.getClass();

        try {
            Method method = certificateClass.getMethod(
                    "getSSLSocketFactory"
            );

            Object result = method.invoke(certificado);

            if (result instanceof SSLSocketFactory) {
                return (SSLSocketFactory) result;
            }

        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException e) {
            log.fine(
                    "[ConsultaTributacao] Erro ao obter SSLSocketFactory: "
                            + e.getMessage()
            );
        }

        try {
            Method method = certificateClass.getMethod(
                    "getSslContext"
            );

            Object result = method.invoke(certificado);

            if (result instanceof SSLContext) {
                return ((SSLContext) result).getSocketFactory();
            }

        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException e) {
            log.fine(
                    "[ConsultaTributacao] Erro ao obter SSLContext: "
                            + e.getMessage()
            );
        }

        return null;
    }

    private static String inputStreamToString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder result = new StringBuilder(4096);
        char[] buffer = new char[4096];

        try (Reader reader = new InputStreamReader(
                inputStream,
                StandardCharsets.UTF_8
        )) {
            int charsRead;

            while ((charsRead = reader.read(buffer)) != -1) {
                result.append(buffer, 0, charsRead);
            }
        }

        return result.toString();
    }

    /**
     * Classe que representa o relatório de validação da estrutura JSON.
     */
    public static class ValidationReport {
        private Set<String> extraFields = new HashSet<>();
        private Set<String> missingFields = new HashSet<>();
        private Map<String, String> typeErrors = new HashMap<>();
        private boolean strictConversionSuccess;
        private String strictConversionError;
        private boolean arrayDetected;
        private int arraySize;

        public Set<String> getExtraFields() { return extraFields; }
        public void setExtraFields(Set<String> value) { extraFields = value; }
        public Set<String> getMissingFields() { return missingFields; }
        public void setMissingFields(Set<String> value) { missingFields = value; }
        public Map<String, String> getTypeErrors() { return typeErrors; }
        public void setTypeErrors(Map<String, String> value) { typeErrors = value; }
        public boolean isStrictConversionSuccess() { return strictConversionSuccess; }
        public void setStrictConversionSuccess(boolean value) { strictConversionSuccess = value; }
        public String getStrictConversionError() { return strictConversionError; }
        public void setStrictConversionError(String value) { strictConversionError = value; }
        public boolean isArrayDetected() { return arrayDetected; }
        public void setArrayDetected(boolean value) { arrayDetected = value; }
        public int getArraySize() { return arraySize; }
        public void setArraySize(int value) { arraySize = value; }

        /**
         * Retorna true se houver qualquer inconsistência detectada.
         */
        public boolean hasIssues() {
            return !extraFields.isEmpty() ||
                    !missingFields.isEmpty() ||
                    !typeErrors.isEmpty() ||
                    !strictConversionSuccess;
        }

        /**
         * Retorna true se não houver nenhum problema.
         */
        public boolean isValid() {
            return !hasIssues();
        }

        /**
         * Gera relatório formatado em texto.
         */
        public String generateReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== RELATORIO DE VALIDACAO ===\n\n");

            if (arrayDetected) {
                sb.append("Array detectado com ").append(arraySize).append(" elemento(s)\n");
                sb.append("Validacao realizada no primeiro elemento\n\n");
            }

            if (extraFields.isEmpty() && missingFields.isEmpty() && typeErrors.isEmpty()) {
                sb.append("ESTRUTURA 100% COMPATIVEL\n");
                sb.append("  - Nenhum campo extra no JSON\n");
                sb.append("  - Nenhum campo faltando\n");
                sb.append("  - Todos os tipos estao corretos\n");
            } else {
                if (!extraFields.isEmpty()) {
                    sb.append("ATENCAO: Campos EXTRAS no JSON (nao mapeados no DTO):\n");
                    for (String field : extraFields) {
                        sb.append("  - ").append(field).append("\n");
                    }
                    sb.append("\n");
                }

                if (!missingFields.isEmpty()) {
                    sb.append("ATENCAO: Campos FALTANDO no JSON (esperados no DTO):\n");
                    for (String field : missingFields) {
                        sb.append("  - ").append(field).append("\n");
                    }
                    sb.append("\n");
                }

                if (!typeErrors.isEmpty()) {
                    sb.append("ERRO: Incompatibilidade de tipos:\n");
                    for (Map.Entry<String, String> entry : typeErrors.entrySet()) {
                        sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    }
                    sb.append("\n");
                }
            }

            sb.append("Conversao estrita: ").append(strictConversionSuccess ? "SUCESSO" : "FALHOU").append("\n");
            if (!strictConversionSuccess && strictConversionError != null) {
                sb.append("  Erro: ").append(strictConversionError).append("\n");
            }

            return sb.toString();
        }

        @Override
        public String toString() {
            return generateReport();
        }
    }
}
