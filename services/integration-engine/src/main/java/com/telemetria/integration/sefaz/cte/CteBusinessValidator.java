package com.telemetria.integration.sefaz.cte;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

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
                context.getMetadata() == null) {

            throw new CteException(
                    "Contexto/metadados do CT-e não encontrados."
            );
        }

        context.setStatus(
                CteStatus.VALIDANDO_REGRAS
        );

        CteMetadata metadata =
                context.getMetadata();

        validarChave(metadata);
        validarModelo(metadata);
        validarNumero(metadata);
        validarSerie(metadata);

        context.setStatus(
                CteStatus.VALIDADO
        );

        exchange.setProperty(
                CteExchangeProperties.CTE_STATUS,
                CteStatus.VALIDADO.name()
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