package com.telemetria.integration.sefaz.cte.pipeline;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.domain.CteContext;
import com.telemetria.integration.sefaz.cte.domain.CteStatus;
import com.telemetria.integration.sefaz.cte.exception.CteException;

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
                context.xmlAssinado();

        if (xml == null || xml.isBlank()) {

            throw new CteException(
                    "XML assinado não encontrado para envio."
            );
        }

        exchange.setProperty(
                CteExchangeProperties.CTE_CONTEXT,
                context.comStatus(CteStatus.ENVIANDO_SEFAZ)
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
                context.metadata().chave()
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
