package com.telemetria.integration.sefaz.cte.soap;

import java.net.URI;
import java.time.Duration;

import com.telemetria.integration.sefaz.cte.exception.CteException;

/**
 * Contrato responsável pelo transporte HTTPS/SOAP dos serviços CT-e.
 *
 * <p>
 * A implementação deve utilizar HTTPS com autenticação mTLS
 * através de certificado digital e retornar a resposta SOAP/XML
 * bruta recebida da SEFAZ.
 * </p>
 *
 * <p>
 * Esta interface não interpreta:
 * </p>
 *
 * <ul>
 *     <li>SOAP Fault;</li>
 *     <li>cStat;</li>
 *     <li>xMotivo;</li>
 *     <li>protocolos;</li>
 *     <li>regras fiscais do CT-e.</li>
 * </ul>
 *
 * <p>
 * Essas responsabilidades pertencem ao {@link CteResponseParser}.
 * </p>
 */
public interface CteSoapTransport {

    /**
     * Timeout genérico utilizado quando a operação
     * não possuir timeout específico.
     */
    Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);

    /**
     * Envia uma requisição SOAP para a SEFAZ.
     *
     * @param soapRequest envelope SOAP completo
     * @param endpoint endpoint HTTPS da SEFAZ
     * @param service serviço SOAP CT-e
     * @param timeout tempo máximo da requisição
     *
     * @return XML/SOAP bruto retornado pela SEFAZ
     *
     * @throws IllegalArgumentException quando parâmetros obrigatórios
     *                                  forem inválidos
     * @throws CteException em caso de erro HTTP, TLS,
     *                      conexão ou timeout
     */
    String enviar(
            String soapRequest,
            URI endpoint,
            CteSoapService service,
            Duration timeout);

    /**
     * Envia utilizando o timeout configurado
     * especificamente para o serviço CT-e.
     */
    default String enviar(
            String soapRequest,
            URI endpoint,
            CteSoapService service) {

        if (service == null) {
            throw new IllegalArgumentException(
                    "O serviço SOAP CT-e deve ser informado.");
        }

        Duration timeout = service.timeout();

        if (timeout == null) {
            timeout = DEFAULT_REQUEST_TIMEOUT;
        }

        return enviar(
                soapRequest,
                endpoint,
                service,
                timeout);
    }
}