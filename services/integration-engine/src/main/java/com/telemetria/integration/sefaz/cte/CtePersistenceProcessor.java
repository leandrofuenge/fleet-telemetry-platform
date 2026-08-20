package com.telemetria.integration.sefaz.cte;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

        CteMetadata metadata =
                exchange.getProperty(
                        CteExchangeProperties.CTE_METADATA,
                        CteMetadata.class
                );

        if (metadata == null) {

            throw new CteException(
                    "Metadados do CT-e não encontrados no Exchange."
            );
        }

        String xml =
                exchange.getMessage()
                        .getBody(String.class);

        if (xml == null || xml.isBlank()) {

            throw new CteException(
                    "XML do CT-e não encontrado para persistência."
            );
        }

        repository.salvarOuAtualizar(
                metadata,
                xml,
                CteStatus.RECEBIDO
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_STATUS,
                CteStatus.RECEBIDO.name()
        );

        log.debug(
                "CT-e persistido com sucesso. chave={}",
                metadata.chave()
        );
    }
}