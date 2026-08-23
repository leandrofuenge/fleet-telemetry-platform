package com.telemetria.integration.datatransfer;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class DataTransferRoute extends RouteBuilder {

    public static final String ROUTE_TRANSFER_BASE64 = "direct:transfer-base64";

    @Override
    public void configure() {

        onException(DataTransferValidationException.class)
            .handled(false);

        onException(Exception.class)
            .handled(true)
            .log("Erro no processamento de transferência Base64/SOAP: ${exception.message}")
            .process("errorHandlingProcessor");

        from(ROUTE_TRANSFER_BASE64)
            .routeId("data-transfer-base64-route")
            .process("auditLogProcessor")
            .log("Executando pipeline de transferência Base64 e contextualização SOAP...")
            .process("base64TransferProcessor")
            .process("auditLogProcessor")
            .log("Pipeline de transferência Base64 concluído com sucesso.");
    }
}
