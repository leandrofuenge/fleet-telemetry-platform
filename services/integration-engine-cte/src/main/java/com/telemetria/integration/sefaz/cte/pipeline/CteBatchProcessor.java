package com.telemetria.integration.sefaz.cte.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.dto.CteBatchResult;
import com.telemetria.integration.sefaz.cte.dto.CteItemResult;
import com.telemetria.integration.sefaz.cte.dto.CteProcessingResult;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.service.CteDocumentProcessor;

@Component("cteBatchProcessor")
public class CteBatchProcessor implements Processor {

    private static final Logger log =
            LoggerFactory.getLogger(CteBatchProcessor.class);

    private final CteDocumentProcessor documentProcessor;

    public CteBatchProcessor(
            CteDocumentProcessor documentProcessor
    ) {
        this.documentProcessor = documentProcessor;
    }

    @Override
    public void process(Exchange exchange) {

        Object body = exchange.getMessage().getBody();

        if (body == null) {
            log.warn("Corpo da mensagem está nulo.");

            exchange.getMessage().setBody(
                    CteBatchResult.vazio()
            );

            return;
        }

        if (!(body instanceof List<?> documentos)) {

            log.warn(
                    "Tipo de mensagem inválido para processamento em lote. tipo={}",
                    body.getClass().getName()
            );

            exchange.getMessage().setBody(
                    CteBatchResult.falhaGeral(
                            "INVALID_BATCH_BODY",
                            "O corpo da mensagem deve ser uma lista de CT-es."
                    )
            );

            return;
        }

        if (documentos.isEmpty()) {

            log.warn(
                    "Nenhum CT-e encontrado no lote."
            );

            exchange.getMessage().setBody(
                    CteBatchResult.vazio()
            );

            return;
        }

        log.info(
                "Iniciando processamento do lote de CT-e. quantidade={}",
                documentos.size()
        );

        List<CteItemResult> resultados =
                new ArrayList<>(documentos.size());

        for (int i = 0; i < documentos.size(); i++) {

            int indice = i + 1;

            Object documento = documentos.get(i);

            CteItemResult resultado =
                    processarItem(documento, indice);

            resultados.add(resultado);
        }

        CteBatchResult resultado =
                CteBatchResult.from(resultados);

        log.info(
                "Processamento do lote finalizado. total={}, sucessos={}, falhas={}",
                resultado.total(),
                resultado.totalSucessos(),
                resultado.totalFalhas()
        );

        exchange.getMessage().setBody(resultado);
    }

    private CteItemResult processarItem(
            Object documento,
            int indice
    ) {

        if (documento == null) {

            return CteItemResult.falha(
                    indice,
                    "NULL_DOCUMENT",
                    "Documento CT-e está nulo."
            );
        }

        if (!(documento instanceof String xmlCte)) {

            log.warn(
                    "Item inválido no lote. indice={}, tipo={}",
                    indice,
                    documento.getClass().getName()
            );

            return CteItemResult.falha(
                    indice,
                    "INVALID_DOCUMENT_TYPE",
                    "O CT-e deve ser informado como XML em formato String."
            );
        }

        if (xmlCte.isBlank()) {

            return CteItemResult.falha(
                    indice,
                    "EMPTY_XML",
                    "O conteúdo XML do CT-e está vazio."
            );
        }

        try {

            log.debug(
                    "Processando CT-e. indice={}",
                    indice
            );

            CteProcessingResult resultado =
                    documentProcessor.process(xmlCte);

            log.info(
                    "CT-e processado com sucesso. indice={}, chave={}, protocolo={}",
                    indice,
                    resultado.chaveAcesso(),
                    resultado.protocolo()
            );

            return CteItemResult.sucesso(
                    indice,
                    resultado
            );

        } catch (CteException e) {

            log.warn(
                    "Falha no processamento do CT-e. indice={}, codigo={}, mensagem={}",
                    indice,
                    e.getCode(),
                    e.getMessage()
            );

            return CteItemResult.falha(
                    indice,
                    e.getCode(),
                    e.getMessage()
            );

        } catch (Exception e) {

            log.error(
                    "Erro inesperado durante processamento do CT-e. indice={}",
                    indice,
                    e
            );

            return CteItemResult.falha(
                    indice,
                    "INTERNAL_ERROR",
                    "Erro interno durante processamento do CT-e."
            );
        }
    }
}