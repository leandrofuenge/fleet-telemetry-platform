package com.telemetria.integration.sefaz.cte.validation;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

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

    private static final String XSD_PATH =
            "xsd/cte/cte_v4.00.xsd";

    private final Schema schema;

    public CteXsdValidator() {

        try {

            SchemaFactory factory =
                    SchemaFactory.newInstance(
                            XMLConstants.W3C_XML_SCHEMA_NS_URI
                    );

            factory.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );

            factory.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    ""
            );

            var resource =
                    getClass()
                            .getClassLoader()
                            .getResource(XSD_PATH);

            if (resource == null) {

                throw new IllegalStateException(
                        "XSD não encontrado: " + XSD_PATH
                );
            }

            this.schema =
                    factory.newSchema(resource);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Não foi possível carregar o XSD do CT-e.",
                    e
            );
        }
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

            var validator =
                    schema.newValidator();

            validator.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );

            validator.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    ""
            );

            validator.validate(
                    new StreamSource(
                            new StringReader(xml)
                    )
            );

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

        } catch (SAXException e) {

            throw new CteException(
                    "CT-e inválido segundo o XSD.",
                    e
            );

        } catch (Exception e) {

            throw new CteException(
                    "Erro durante validação XSD do CT-e.",
                    e
            );
        }
    }
}
