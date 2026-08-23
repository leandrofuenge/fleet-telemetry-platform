package com.telemetria.integration.sefaz.cte.validation;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.domain.CteContext;
import com.telemetria.integration.sefaz.cte.domain.CteMetadata;
import com.telemetria.integration.sefaz.cte.domain.CteStatus;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.pipeline.CteExchangeProperties;

@Component("cteBusinessValidator")
public class CteBusinessValidator
        implements Processor {

    @Override
    public void process(
            Exchange exchange
    ) {

        CteContext context =
                exchange.getProperty(
                        CteExchangeProperties.CTE_CONTEXT,
                        CteContext.class
                );

        if (context == null ||
                context.metadata() == null) {

            throw new CteException(
                    "Contexto/metadados do CT-e não encontrados."
            );
        }

        CteMetadata metadata =
                context.metadata();

        validarChave(metadata);
        validarModelo(metadata);
        validarNumero(metadata);
        validarSerie(metadata);

        exchange.setProperty(
                CteExchangeProperties.CTE_CONTEXT,
                context.comStatus(CteStatus.REGRAS_VALIDAS)
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_STATUS,
                CteStatus.REGRAS_VALIDAS.name()
        );
    }

    private void validarChave(
            CteMetadata metadata
    ) {

        if (metadata.chave() == null ||
                !metadata.chave().matches("\\d{44}")) {

            throw new CteException(
                    "CTE_CHAVE_INVALIDA",
                    "Chave do CT-e deve possuir 44 dígitos."
            );
        }
    }

    private void validarModelo(
            CteMetadata metadata
    ) {

        if (metadata.modelo() == null ||
                metadata.modelo().isBlank()) {

            throw new CteException(
                    "CTE_MODELO_INVALIDO",
                    "Modelo do CT-e não informado."
            );
        }
    }

    private void validarNumero(
            CteMetadata metadata
    ) {

        if (metadata.numero() == null ||
                metadata.numero().isBlank()) {

            throw new CteException(
                    "CTE_NUMERO_INVALIDO",
                    "Número do CT-e não informado."
            );
        }
    }

    private void validarSerie(
            CteMetadata metadata
    ) {

        if (metadata.serie() == null ||
                metadata.serie().isBlank()) {

            throw new CteException(
                    "CTE_SERIE_INVALIDA",
                    "Série do CT-e não informada."
            );
        }
    }
}
