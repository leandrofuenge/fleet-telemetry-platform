package com.telemetria.integration.sefaz.cte;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("cteSefazSender")
public class CteSefazSender
        implements Processor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CteSefazSender.class
            );

    @Override
    public void process(
            Exchange exchange
    ) {

        CteContext context =
                obterContexto(exchange);

        String xml =
                context.getXmlAssinado();

        if (xml == null || xml.isBlank()) {

            throw new CteException(
                    "XML assinado não encontrado para envio."
            );
        }

        context.setStatus(
                CteStatus.ENVIANDO_SEFAZ
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_STATUS,
                CteStatus.ENVIANDO_SEFAZ.name()
        );

        /*
         * TODO:
         *
         * Implementar chamada ao WebService
         * da SEFAZ correspondente ao ambiente/UF.
         *
         * O endpoint e o envelope SOAP dependem
         * da configuração do projeto.
         */

        log.info(
                "Preparando envio do CT-e para SEFAZ. chave={}",
                context.getMetadata().chave()
        );

        throw new CteException(
                "Integração com SEFAZ ainda não configurada."
        );
    }

    private CteContext obterContexto(
            Exchange exchange
    ) {

        CteContext context =
                exchange.getProperty(
                        CteExchangeProperties.CTE_CONTEXT,
                        CteContext.class
                );

        if (context == null) {

            throw new CteException(
                    "Contexto do CT-e não encontrado."
            );
        }

        return context;
    }
}