package com.telemetria.integration.sefaz.cte.pipeline;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.domain.CteContext;
import com.telemetria.integration.sefaz.cte.domain.CteStatus;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.persistence.CteRepository;

@Component("ctePersistenceProcessor")
public class CtePersistenceProcessor implements Processor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CtePersistenceProcessor.class
            );

    private final CteRepository repository;

    public CtePersistenceProcessor(
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

        if (context == null || context.metadata() == null) {

            throw new CteException(
                    "Metadados do CT-e não encontrados no Exchange."
            );
        }

        String xml = context.xmlNormalizado();

        if (xml == null || xml.isBlank()) {

            throw new CteException(
                    "XML do CT-e não encontrado para persistência."
            );
        }

        CteRepository.CtePersistenceData persistence =
                repository.salvarOuAtualizar(context);

        CteContext persistedContext = new CteContext(
                persistence.id(),
                context.metadata(),
                context.xmlOriginal(),
                context.xmlNormalizado(),
                context.xmlAssinado(),
                context.xmlHash(),
                context.status(),
                persistence.tentativa()
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_CONTEXT,
                persistedContext
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_STATUS,
                CteStatus.RECEBIDO.name()
        );

        log.debug(
                "CT-e persistido com sucesso. chave={}",
                context.metadata().chave()
        );
    }
}
