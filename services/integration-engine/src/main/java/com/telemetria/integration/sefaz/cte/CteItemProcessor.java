package com.telemetria.integration.sefaz.cte;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("cteItemProcessor")
public class CteItemProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // Recebe o item individual que foi dividido pelo Splitter
        String xmlItem = exchange.getIn().getBody(String.class);
        
        // Aplica a lógica unitária por item
        System.out.println("Processando item do lote: " + xmlItem);
    }
}
