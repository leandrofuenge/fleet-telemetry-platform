package com.telemetria.integration.support;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;

@Component("errorHandlingProcessor")
public class ErrorHandlingProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingProcessor.class);

    @Override
    public void process(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String uf = exchange.getProperty("SEFAZ_UF", "MT", String.class);
        String ambiente = exchange.getProperty("SEFAZ_AMBIENTE_NOME", "HOMOLOGACAO", String.class);
        Long startTime = exchange.getProperty(AuditLogProcessor.HEADER_START_TIME, Long.class);
        long duration = (startTime != null) ? (System.currentTimeMillis() - startTime) : 0L;

        String errorMessage = (exception != null) ? exception.getMessage() : "Erro desconhecido na comunicação com SEFAZ";
        log.error("[Camel ErrorHandler] Falha na integração com SEFAZ UF: {} | Detalhe: {}", uf, errorMessage);

        CteStatusResponse errorResponse = new CteStatusResponse();
        errorResponse.setSistema("SEFAZ");
        errorResponse.setDocumento("CTE");
        errorResponse.setAmbiente(ambiente);
        errorResponse.setUf(uf);
        errorResponse.setDisponivel(false);
        errorResponse.setCodigo("500");
        errorResponse.setMensagem("Falha de comunicação com autorizador SEFAZ: " + errorMessage);
        errorResponse.setTempoRespostaMs(duration);

        exchange.getIn().setBody(errorResponse);
    }
}
