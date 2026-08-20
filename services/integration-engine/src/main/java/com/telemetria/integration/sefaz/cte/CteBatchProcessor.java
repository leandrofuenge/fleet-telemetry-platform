package com.telemetria.integration.sefaz.cte;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Processador de lote para agrupamento e validação/envio sequencial de CT-es agregados via Apache Camel.
 */
@Component("cteBatchProcessor")
public class CteBatchProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(CteBatchProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        List<?> rawList = exchange.getMessage().getBody(List.class);

        if (rawList == null || rawList.isEmpty()) {
            log.warn("Nenhum CT-e encontrado no corpo da mensagem para processamento em lote.");
            exchange.getMessage().setBody(new CteBatchResult(0, 0, List.of()));
            return;
        }

        List<String> loteCtes = rawList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();

        log.info("Iniciando processamento em lote de {} CT-es...", loteCtes.size());

        int sucessos = 0;
        List<String> falhas = new ArrayList<>();

        for (int i = 0; i < loteCtes.size(); i++) {
            String xmlCte = loteCtes.get(i);
            try {
                processarDocumentoIndividual(xmlCte, i + 1);
                sucessos++;
            } catch (Exception e) {
                log.error("Erro ao processar o CT-e #{} no lote: {}", i + 1, e.getMessage(), e);
                falhas.add("Item " + (i + 1) + ": " + e.getMessage());
            }
        }

        CteBatchResult resultado = new CteBatchResult(sucessos, falhas.size(), falhas);
        log.info("Lote finalizado com sucesso. Sucessos: {}, Falhas: {}", sucessos, falhas.size());

        exchange.getMessage().setBody(resultado);
    }

    private void processarDocumentoIndividual(String xmlCte, int indice) {
        log.debug("Processando documento #{} do lote...", indice);
        if (xmlCte == null || xmlCte.isBlank()) {
            throw new CteException("Conteúdo XML do CT-e é nulo ou está em branco.");
        }
        // TODO: Injetar serviço de negócio (ex: CteService, XmlSigner) para execução real
    }

    /**
     * DTO imutável de resultado consolidado do processamento em lote.
     */
    public record CteBatchResult(int totalSucessos, int totalFalhas, List<String> erros) {}
}