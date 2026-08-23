package com.telemetria.integration.sefaz.cte.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.exception.CteException;

@Component
public class CteRoute extends RouteBuilder {

    public static final String ROUTE_CTE_STATUS = "direct:cte-status";

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

        from(ROUTE_CTE_STATUS)
                .routeId("cte-status-servico")
                .bean("cteStatusService", "consultar");
    }
}
