package com.telemetria.integration.sefaz.cte.validation;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.domain.CteContext;
import com.telemetria.integration.sefaz.cte.domain.CteStatus;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.pipeline.CteExchangeProperties;

@Component("cteXsdValidator")
public class CteXsdValidator implements Processor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CteXsdValidator.class
            );

    private final CteXmlValidator xmlValidator;

    public CteXsdValidator(CteXmlValidator xmlValidator) {
        this.xmlValidator = xmlValidator;
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

            xmlValidator.validarCte(xml);

            CteContext novoContext =
                    context.comStatus(
                            CteStatus.XSD_VALIDO
                    );

            exchange.setProperty(
                    CteExchangeProperties.CTE_CONTEXT,
                    novoContext
            );

            exchange.setProperty(
                    CteExchangeProperties.CTE_STATUS,
                    CteStatus.XSD_VALIDO.name()
            );

            log.info(
                    "XSD validado com sucesso. chave={}",
                    context.metadata().chave()
            );

        } catch (Exception e) {

            throw new CteException(
                    "Erro durante validação XSD do CT-e.",
                    e
            );
        }
    }
}
