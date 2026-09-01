package com.telemetria.integration.nfe.soap;

import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.nfe.config.NfeProperties;

/**
 * Gateway responsável por orquestrar a comunicação SOAP da NF-e.
 *
 * Responsabilidades:
 * - validar os parâmetros básicos da operação;
 * - criar o envelope SOAP;
 * - enviar a requisição através do transporte HTTP/mTLS;
 * - validar a resposta SOAP;
 * - registrar informações de diagnóstico sem expor dados fiscais.
 *
 * Não é responsabilidade desta classe:
 * - montar regras de negócio da NF-e;
 * - interpretar o conteúdo específico do XML fiscal;
 * - realizar comunicação HTTP diretamente;
 * - configurar SSL/mTLS.
 */
@Component
public class NfeSoapGateway {

    private static final Logger log =
            LoggerFactory.getLogger(NfeSoapGateway.class);

    private final NfeSoapEnvelopeFactory envelopeFactory;
    private final NfeSoapTransport transport;
    private final NfeSoapResponseValidator responseValidator;
    private final NfeProperties properties;

    public NfeSoapGateway(
            NfeSoapEnvelopeFactory envelopeFactory,
            NfeSoapTransport transport,
            NfeSoapResponseValidator responseValidator,
            NfeProperties properties) {

        this.envelopeFactory = envelopeFactory;
        this.transport = transport;
        this.responseValidator = responseValidator;
        this.properties = properties;
    }

    /**
     * Executa uma operação SOAP contra a SEFAZ.
     *
     * Fluxo:
     *
     * 1. valida parâmetros;
     * 2. cria envelope SOAP;
     * 3. envia requisição;
     * 4. valida resposta SOAP;
     * 5. retorna XML da resposta.
     *
     * @param service operação SOAP
     * @param endpoint endpoint HTTPS da SEFAZ
     * @param xmlFiscal XML fiscal
     * @return resposta SOAP da SEFAZ
     */
    public String enviar(
            NfeSoapService service,
            URI endpoint,
            String xmlFiscal) {

        validarParametros(
                service,
                endpoint,
                xmlFiscal);

        Duration timeout =
                obterTimeout();

        log.debug(
                "NF-e: iniciando operação SOAP [service={}, timeout={}ms]",
                service.name(),
                timeout.toMillis());

        String envelopeSoap =
                envelopeFactory.criar(
                        service,
                        xmlFiscal);

        log.debug(
                "NF-e: envelope SOAP criado [service={}]",
                service.name());

        String respostaSoap =
                transport.enviar(
                        envelopeSoap,
                        endpoint,
                        service,
                        timeout);

        log.debug(
                "NF-e: resposta recebida [service={}]",
                service.name());

        responseValidator.validar(
                respostaSoap,
                service);

        log.debug(
                "NF-e: resposta SOAP validada com sucesso [service={}]",
                service.name());

        return respostaSoap;
    }

    private Duration obterTimeout() {

        int timeoutMillis =
                properties.getTimeoutMillis();

        if (timeoutMillis <= 0) {

            throw new IllegalStateException(
                    "Timeout da integração NF-e deve ser maior que zero.");
        }

        return Duration.ofMillis(timeoutMillis);
    }

    private void validarParametros(
            NfeSoapService service,
            URI endpoint,
            String xmlFiscal) {

        if (service == null) {

            throw new IllegalArgumentException(
                    "Serviço SOAP da NF-e não pode ser nulo.");
        }

        if (endpoint == null) {

            throw new IllegalArgumentException(
                    "Endpoint da SEFAZ NF-e não pode ser nulo.");
        }

        if (!"https".equalsIgnoreCase(
                endpoint.getScheme())) {

            throw new IllegalArgumentException(
                    "Endpoint da SEFAZ NF-e deve utilizar HTTPS.");
        }

        if (endpoint.getHost() == null
                || endpoint.getHost().isBlank()) {

            throw new IllegalArgumentException(
                    "Host do endpoint da SEFAZ NF-e não pode ser vazio.");
        }

        if (xmlFiscal == null
                || xmlFiscal.isBlank()) {

            throw new IllegalArgumentException(
                    "XML fiscal da NF-e não pode ser vazio.");
        }
    }
}