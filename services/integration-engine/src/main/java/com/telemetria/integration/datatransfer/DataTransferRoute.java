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
            // Entrada: cria ou reaproveita a correlação e registra o início.
            .process("auditLogProcessor")
            .log("Executando pipeline de transferência: decodificação, validação, transformação e resposta.")
            // Processamento: o processador coordena componentes especializados.
            .process("base64TransferProcessor")
            // Saída: registra o encerramento com a mesma correlação.
            .process("auditLogProcessor")
            .log("Pipeline de transferência Base64 concluído com sucesso.");
    }
}
