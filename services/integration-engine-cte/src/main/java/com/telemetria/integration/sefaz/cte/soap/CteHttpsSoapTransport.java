package com.telemetria.integration.sefaz.cte.soap;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.exception.CteException;

/**
 * Implementação HTTPS/SOAP 1.2 para comunicação com
 * os WebServices CT-e da SEFAZ.
 *
 * <p>
 * Responsabilidades desta classe:
 * </p>
 *
 * <ul>
 *     <li>HTTPS;</li>
 *     <li>mTLS;</li>
 *     <li>HTTP 1.1;</li>
 *     <li>timeout;</li>
 *     <li>envio da requisição;</li>
 *     <li>validação da camada HTTP;</li>
 *     <li>retorno do XML SOAP bruto.</li>
 * </ul>
 *
 * <p>
 * Esta classe NÃO interpreta SOAP Fault nem códigos
 * de retorno fiscais. Essa responsabilidade pertence
 * ao {@link CteResponseParser}.
 * </p>
 */
@Component
public class CteHttpsSoapTransport implements CteSoapTransport {

    private static final Logger log =
            LoggerFactory.getLogger(CteHttpsSoapTransport.class);

    /**
     * Timeout para estabelecimento da conexão TCP/TLS.
     *
     * Diferente do timeout individual de cada requisição.
     */
    private static final Duration CONNECT_TIMEOUT =
            Duration.ofSeconds(15);

    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 299;

    private final HttpClient httpClient;

    /**
     * Construtor utilizado pelo Spring.
     */
    @Autowired
    public CteHttpsSoapTransport(
            @Qualifier("sefazSslContext")
            SSLContext sslContext) {

        if (sslContext == null) {
            throw new IllegalArgumentException(
                    "SSLContext da SEFAZ não pode ser nulo.");
        }

        this.httpClient =
                buildHttpClient(sslContext);
    }

    /**
     * Construtor destinado principalmente aos testes.
     *
     * Permite injetar HttpClient mockado.
     */
    CteHttpsSoapTransport(
            HttpClient httpClient) {

        if (httpClient == null) {
            throw new IllegalArgumentException(
                    "HttpClient não pode ser nulo.");
        }

        this.httpClient = httpClient;
    }

    @Override
    public String enviar(
            String soapRequest,
            URI endpoint,
            CteSoapService service,
            Duration timeout) {

        validarParametros(
                soapRequest,
                endpoint,
                service,
                timeout);

        HttpRequest request =
                criarRequest(
                        soapRequest,
                        endpoint,
                        service,
                        timeout);

        return executarRequest(
                request,
                endpoint,
                service,
                timeout);
    }

    /**
     * Executa a chamada HTTP e converte falhas técnicas em exceções
     * de domínio da integração CT-e.
     */
    private String executarRequest(
            HttpRequest request,
            URI endpoint,
            CteSoapService service,
            Duration timeout) {

        long inicio =
                System.nanoTime();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8));

            long duracaoMs =
                    Duration.ofNanos(
                                    System.nanoTime() - inicio)
                            .toMillis();

            registrarResposta(
                    service,
                    endpoint,
                    response.statusCode(),
                    duracaoMs);

            validarStatusHttp(
                    response.statusCode(),
                    service);

            String corpo =
                    response.body();

            validarCorpo(
                    corpo,
                    response.statusCode(),
                    service);

            /*
             * Importante:
             *
             * Não interpretamos SOAP Fault aqui.
             *
             * Mesmo sendo SOAP Fault, HTTP 200 pode ser retornado.
             * O XML seguirá para CteResponseParser.
             */
            return corpo;

        } catch (HttpConnectTimeoutException e) {

            log.error(
                    "Timeout ao conectar com a SEFAZ. "
                            + "operacao={} endpoint={}",
                    service.soapAction(),
                    endpoint.getHost());

            throw new CteException(
                    "Timeout ao estabelecer conexão com a SEFAZ "
                            + "para a operação "
                            + service.soapAction()
                            + ".",
                    e);

        } catch (HttpTimeoutException e) {

            log.error(
                    "Timeout aguardando resposta da SEFAZ. "
                            + "operacao={} endpoint={} timeout={}",
                    service.soapAction(),
                    endpoint.getHost(),
                    timeout);

            throw new CteException(
                    "Timeout aguardando resposta da SEFAZ "
                            + "para a operação "
                            + service.soapAction()
                            + ".",
                    e);

        } catch (SSLHandshakeException e) {

            log.error(
                    "Falha no handshake TLS/mTLS com a SEFAZ. "
                            + "operacao={} endpoint={} erro={}",
                    service.soapAction(),
                    endpoint.getHost(),
                    e.getMessage());

            throw new CteException(
                    "Falha no handshake TLS/mTLS com a SEFAZ. "
                            + "Verifique certificado digital, validade, "
                            + "cadeia ICP-Brasil e configuração SSL.",
                    e);

        } catch (ConnectException e) {

            log.error(
                    "Não foi possível conectar à SEFAZ. "
                            + "operacao={} endpoint={} erro={}",
                    service.soapAction(),
                    endpoint.getHost(),
                    e.getMessage());

            throw new CteException(
                    "Não foi possível estabelecer conexão "
                            + "com o WebService da SEFAZ.",
                    e);

        } catch (InterruptedException e) {

            /*
             * Nunca devemos consumir silenciosamente
             * uma interrupção da thread.
             */
            Thread.currentThread().interrupt();

            log.warn(
                    "Thread interrompida durante comunicação "
                            + "com SEFAZ. operacao={}",
                    service.soapAction());

            throw new CteException(
                    "Comunicação com a SEFAZ interrompida "
                            + "durante a operação "
                            + service.soapAction()
                            + ".",
                    e);

        } catch (CteException e) {

            /*
             * Exceções já classificadas pela aplicação
             * não devem ser encapsuladas novamente.
             */
            throw e;

        } catch (IOException e) {

            log.error(
                    "Erro de I/O na comunicação com a SEFAZ. "
                            + "operacao={} endpoint={} tipo={} erro={}",
                    service.soapAction(),
                    endpoint.getHost(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);

            throw new CteException(
                    "Falha de comunicação com o WebService da SEFAZ.",
                    e);

        } catch (Exception e) {

            log.error(
                    "Erro inesperado na comunicação SEFAZ. "
                            + "operacao={} endpoint={} tipo={} erro={}",
                    service.soapAction(),
                    endpoint.getHost(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);

            throw new CteException(
                    "Erro inesperado durante comunicação "
                            + "com a SEFAZ CT-e.",
                    e);
        }
    }

    /**
     * Cria a requisição HTTP/SOAP 1.2.
     */
    private HttpRequest criarRequest(
            String soapRequest,
            URI endpoint,
            CteSoapService service,
            Duration timeout) {

        return HttpRequest
                .newBuilder(endpoint)

                /*
                 * Timeout específico da operação.
                 */
                .timeout(timeout)

                /*
                 * SEFAZ trabalha tradicionalmente
                 * com HTTP/1.1.
                 */
                .version(
                        HttpClient.Version.HTTP_1_1)

                .header(
                        "Content-Type",
                        service.contentType())

                .header(
                        "Accept",
                        "application/soap+xml")

                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                soapRequest,
                                StandardCharsets.UTF_8))

                .build();
    }

    /**
     * Validação dos parâmetros de entrada.
     */
    private void validarParametros(
            String soapRequest,
            URI endpoint,
            CteSoapService service,
            Duration timeout) {

        if (soapRequest == null
                || soapRequest.isBlank()) {

            throw new IllegalArgumentException(
                    "O payload SOAP não pode ser nulo ou vazio.");
        }

        if (endpoint == null) {

            throw new IllegalArgumentException(
                    "O endpoint da SEFAZ deve ser informado.");
        }

        if (!"https".equalsIgnoreCase(
                endpoint.getScheme())) {

            throw new IllegalArgumentException(
                    "O endpoint da SEFAZ deve utilizar HTTPS: "
                            + endpoint);
        }

        if (endpoint.getHost() == null
                || endpoint.getHost().isBlank()) {

            throw new IllegalArgumentException(
                    "Host do endpoint da SEFAZ inválido: "
                            + endpoint);
        }

        if (service == null) {

            throw new IllegalArgumentException(
                    "O serviço SOAP CT-e deve ser informado.");
        }

        if (service.soapAction() == null
                || service.soapAction().isBlank()) {

            throw new IllegalStateException(
                    "SOAP Action não configurada para o serviço "
                            + service
                            + ".");
        }

        if (timeout == null) {

            throw new IllegalArgumentException(
                    "O timeout da requisição deve ser informado.");
        }

        if (timeout.isZero()
                || timeout.isNegative()) {

            throw new IllegalArgumentException(
                    "O timeout da requisição deve ser maior que zero.");
        }
    }

    /**
     * Valida código HTTP retornado pela SEFAZ.
     */
    private void validarStatusHttp(
            int statusCode,
            CteSoapService service) {

        if (statusCode >= HTTP_SUCCESS_MIN
                && statusCode <= HTTP_SUCCESS_MAX) {

            return;
        }

        switch (statusCode) {

            case 400 ->
                    throw new CteException(
                            "SEFAZ rejeitou a requisição HTTP "
                                    + "da operação "
                                    + service.soapAction()
                                    + " (HTTP 400 - Bad Request).");

            case 401 ->
                    throw new CteException(
                            "SEFAZ retornou HTTP 401. "
                                    + "Falha de autenticação.");

            case 403 ->
                    throw new CteException(
                            "SEFAZ retornou HTTP 403. "
                                    + "Acesso ao serviço não autorizado.");

            case 404 ->
                    throw new CteException(
                            "Endpoint da SEFAZ não encontrado "
                                    + "para a operação "
                                    + service.soapAction()
                                    + " (HTTP 404).");

            case 408 ->
                    throw new CteException(
                            "SEFAZ retornou HTTP 408 "
                                    + "(Request Timeout).");

            case 429 ->
                    throw new CteException(
                            "SEFAZ limitou temporariamente "
                                    + "as requisições (HTTP 429).");

            case 502 ->
                    throw new CteException(
                            "Gateway da SEFAZ retornou HTTP 502.");

            case 503 ->
                    throw new CteException(
                            "Serviço da SEFAZ temporariamente "
                                    + "indisponível (HTTP 503).");

            case 504 ->
                    throw new CteException(
                            "Gateway da SEFAZ excedeu "
                                    + "o tempo de resposta (HTTP 504).");

            default -> {

                if (statusCode >= 500) {

                    throw new CteException(
                            "Erro interno ou indisponibilidade "
                                    + "da SEFAZ. HTTP "
                                    + statusCode
                                    + ".");
                }

                throw new CteException(
                        "SEFAZ retornou HTTP "
                                + statusCode
                                + " para a operação "
                                + service.soapAction()
                                + ".");
            }
        }
    }

    /**
     * A camada HTTP respondeu com sucesso,
     * portanto deve existir corpo SOAP/XML.
     */
    private void validarCorpo(
            String corpo,
            int statusCode,
            CteSoapService service) {

        if (corpo == null
                || corpo.isBlank()) {

            throw new CteException(
                    "SEFAZ retornou HTTP "
                            + statusCode
                            + " sem corpo SOAP para a operação "
                            + service.soapAction()
                            + ".");
        }
    }

    /**
     * Registra informações técnicas da requisição.
     *
     * O XML não é registrado para evitar exposição
     * de dados fiscais ou informações sensíveis.
     */
    private void registrarResposta(
            CteSoapService service,
            URI endpoint,
            int statusCode,
            long duracaoMs) {

        log.info(
                "SEFAZ CT-e operação={} host={} status={} tempoMs={}",
                service.name(),
                endpoint.getHost(),
                statusCode,
                duracaoMs);
    }

    /**
     * Cria o HttpClient compartilhado.
     *
     * <p>
     * O HttpClient deve ser reutilizado para aproveitar:
     * </p>
     *
     * <ul>
     *     <li>pool de conexões;</li>
     *     <li>sessões TLS;</li>
     *     <li>keep-alive;</li>
     *     <li>redução de handshakes mTLS.</li>
     * </ul>
     */
    private static HttpClient buildHttpClient(
            SSLContext sslContext) {

        return HttpClient
                .newBuilder()

                .sslContext(
                        sslContext)

                .connectTimeout(
                        CONNECT_TIMEOUT)

                .version(
                        HttpClient.Version.HTTP_1_1)

                /*
                 * Não seguimos redirect automaticamente
                 * em serviços fiscais.
                 */
                .followRedirects(
                        HttpClient.Redirect.NEVER)

                .build();
    }
}
