package com.telemetria.integration.sefaz.cte;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CteRoute extends RouteBuilder {

    @Override
    public void configure() {

        onException(CteException.class)
                .handled(false)
                .log(
                        "Erro no processamento CT-e: ${exception.message}"
                );

        from("direct:processarCteLote")
                .routeId("cte-lote-processamento")

                .split(body())
                    .stopOnException(false)

                    .process("cteItemProcessor")

                    .process("ctePersistenceProcessor")

                    .process("cteXsdValidator")

                    .process("cteBusinessValidator")

                    .process("cteSignatureProcessor")

                    .process("cteSefazSender")

                    .process("cteSefazResponseProcessor")

                .end();
    }
}