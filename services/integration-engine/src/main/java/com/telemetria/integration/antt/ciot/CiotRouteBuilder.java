package com.telemetria.integration.antt.ciot;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "integration.experimental-routes.enabled", havingValue = "true")
public class CiotRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
            .handled(true)
            .log("Erro no fluxo do CIOT: ${exception.message}")
            .setBody(simple("{\"status\": \"ERRO\", \"etapa\": \"GERACAO_CIOT\", \"mensagem\": \"${exception.message}\"}"));

        // Rota de emissão do CIOT
        from("direct:gerarCiot")
            .routeId("rota-geracao-ciot")
            .log("Iniciando geração de CIOT para o veículo ${body.placaVeiculo}...")
            
            // Chama a implementação REST do cliente
            .bean("ciotClient", "gerarCiot")
            
            // Valida se a resposta foi sucesso
            .choice()
                .when(simple("${body.sucesso} == true"))
                    .log("CIOT Gerado com Sucesso! Número: ${body.numeroCiot}")
                    // Armazena o número do CIOT no Header do Camel para ser usado depois na geração do XML do CT-e/MDF-e
                    .setHeader("NUMERO_CIOT", simple("${body.numeroCiot}"))
                .otherwise()
                    .log("Falha na geração do CIOT: ${body.mensagemErro}")
                    .throwException(new RuntimeException("Geração de CIOT rejeitada: ${body.mensagemErro}"))
            .end();


        // Rota de encerramento (chamada ao final da viagem)
        from("direct:encerrarCiot")
            .routeId("rota-encerramento-ciot")
            .log("Solicitando encerramento do CIOT ${header.numeroCiot}...")
            .bean("ciotClient", "encerrarCiot(${header.numeroCiot})")
            .log("Status de encerramento: ${body.sucesso}");
    }
}
