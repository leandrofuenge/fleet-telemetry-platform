package com.telemetria.integration.sefaz.cte;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

            context.setStatus(
                    CteStatus.AUTORIZADO
            );

            context.setCodigoSefaz(codigo);
            context.setMotivoSefaz(motivo);
            context.setProtocolo(protocolo);

            repository.atualizarRespostaSefaz(
                    context.getMetadata().chave(),
                    CteStatus.AUTORIZADO,
                    codigo,
                    motivo,
                    protocolo
            );

            CteProcessResult result =
                    CteProcessResult.sucesso(
                            context.getMetadata(),
                            protocolo
                    );

            exchange.setProperty(
                    CteExchangeProperties.CTE_RESULTADO,
                    result
            );

            log.info(
                    "CT-e autorizado. chave={}, protocolo={}",
                    context.getMetadata().chave(),
                    protocolo
            );

            return;
        }

        context.setStatus(
                CteStatus.REJEITADO
        );

        context.setCodigoSefaz(codigo);
        context.setMotivoSefaz(motivo);

        repository.atualizarRespostaSefaz(
                context.getMetadata().chave(),
                CteStatus.REJEITADO,
                codigo,
                motivo,
                protocolo
        );

        CteProcessResult result =
                CteProcessResult.erro(
                        context.getMetadata(),
                        CteStatus.REJEITADO,
                        codigo,
                        motivo
                );

        exchange.setProperty(
                CteExchangeProperties.CTE_RESULTADO,
                result
        );
    }
}