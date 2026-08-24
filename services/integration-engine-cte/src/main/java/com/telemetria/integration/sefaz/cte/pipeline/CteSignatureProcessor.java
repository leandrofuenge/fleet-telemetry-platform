package com.telemetria.integration.sefaz.cte.pipeline;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.domain.CteContext;
import com.telemetria.integration.sefaz.cte.domain.CteStatus;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.security.CteXmlSigner;

/**
 * Processor responsável pela assinatura digital do CT-e.
 *
 * A implementação concreta da assinatura deve ser injetada
 * posteriormente conforme o mecanismo de certificado utilizado.
 */
@Component("cteSignatureProcessor")
public class CteSignatureProcessor
        implements Processor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CteSignatureProcessor.class
            );

    private final CteXmlSigner xmlSigner;

    public CteSignatureProcessor(
            CteXmlSigner xmlSigner
    ) {
        this.xmlSigner = xmlSigner;
    }

    @Override
    public void process(
            Exchange exchange
    ) {

        CteContext context =
                exchange.getProperty(
                        CteExchangeProperties.CTE_CONTEXT,
                        CteContext.class
                );

        if (context == null) {

            throw new CteException(
                    "CteContext não encontrado."
            );
        }

        String xml =
                context.xmlNormalizado();

        try {

            CteContext assinando =
                    context.comStatus(
                            CteStatus.ASSINANDO
                    );

            exchange.setProperty(
                    CteExchangeProperties.CTE_CONTEXT,
                    assinando
            );

            String xmlAssinado =
                    xmlSigner.assinarXml(xml, "infCte");

            if (xmlAssinado == null ||
                    xmlAssinado.isBlank()) {

                throw new CteException(
                        "O assinador retornou XML vazio."
                );
            }

            CteContext finalContext =
                    assinando
                            .comXmlAssinado(xmlAssinado)
                            .comStatus(
                                    CteStatus.ASSINADO
                            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_CONTEXT,
                    finalContext
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_XML_ASSINADO,
                    xmlAssinado
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_STATUS,
                    CteStatus.ASSINADO.name()
            );

            exchange.getMessage()
                    .setBody(xmlAssinado);

            log.info(
                    "CT-e assinado com sucesso. chave={}",
                    context.metadata().chave()
            );

        } catch (CteException e) {

            throw e;

        } catch (Exception e) {

            throw new CteException(
                    "Erro durante assinatura digital do CT-e.",
                    e
            );
        }
    }
}
