package com.telemetria.integration.sefaz.cte.pipeline;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.domain.CteContext;
import com.telemetria.integration.sefaz.cte.domain.CteProcessResult;
import com.telemetria.integration.sefaz.cte.domain.CteStatus;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.persistence.CteRepository;

@Component("cteSefazResponseProcessor")
public class CteSefazResponseProcessor
        implements Processor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CteSefazResponseProcessor.class
            );

    private final CteRepository repository;

    public CteSefazResponseProcessor(
            CteRepository repository
    ) {
        this.repository = repository;
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
                    "Contexto do CT-e não encontrado."
            );
        }

        /*
         * TODO:
         *
         * Ler resposta real da SEFAZ.
         */

        String codigo =
                exchange.getProperty(
                        "cte.sefaz.codigo",
                        String.class
                );

        String motivo =
                exchange.getProperty(
                        "cte.sefaz.motivo",
                        String.class
                );

        String protocolo =
                exchange.getProperty(
                        "cte.sefaz.protocolo",
                        String.class
                );

        if ("100".equals(codigo)) {

            CteProcessResult result =
                    CteProcessResult.autorizado(
                            context.metadata().chave(),
                            protocolo,
                            codigo,
                            motivo
                    );

            exchange.setProperty(
                    CteExchangeProperties.CTE_CONTEXT,
                    context.comStatus(CteStatus.AUTORIZADO)
            );

            repository.registrarRespostaSefaz(
                    context.metadata().chave(),
                    result
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_RESULTADO,
                    result
            );

            log.info(
                    "CT-e autorizado. chave={}, protocolo={}",
                    context.metadata().chave(),
                    protocolo
            );

            return;
        }

        CteProcessResult result =
                CteProcessResult.rejeitado(
                        context.metadata().chave(),
                        codigo,
                        motivo
                );

        exchange.setProperty(
                CteExchangeProperties.CTE_CONTEXT,
                context.comStatus(CteStatus.REJEITADO)
        );

        repository.registrarRespostaSefaz(
                context.metadata().chave(),
                result
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_RESULTADO,
                result
        );
    }
}
