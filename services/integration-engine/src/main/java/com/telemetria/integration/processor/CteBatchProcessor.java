package com.telemetria.integration.processor;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("cteBatchProcessor")
public class CteBatchProcessor implements Processor {

    @SuppressWarnings("unused")
	@Override
    public void process(Exchange exchange) throws Exception {
        // Recebe a lista de mensagens/XMLs agrupados pelo Aggregator
        @SuppressWarnings("unchecked")
        List<String> loteCtes = exchange.getIn().getBody(List.class);

        if (loteCtes != null && !loteCtes.isEmpty()) {
            System.out.println("Processando lote de " + loteCtes.size() + " CT-es...");

            // Aqui você executa o processamento em lote
            for (String xmlCte : loteCtes) {
                // Exemplo: chamar o CteClient ou validar cada documento
            }

            // Define o retorno consolidado
            exchange.getIn().setBody("Lote de " + loteCtes.size() + " processado com sucesso.");
        }
    }
}