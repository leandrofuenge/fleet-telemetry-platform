package com.telemetria.integration.antt.rntrc;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "integration.experimental-routes.enabled", havingValue = "true")
public class RntrcRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
            .handled(true)
            .log("Erro na integração ANTT RNTRC: ${exception.message}")
            .setBody(simple("{\"status\": \"ERRO\", \"mensagem\": \"${exception.message}\"}"));

        // Rota de Reenvio Sob Demanda ANTT -> SEFAZ
        from("direct:solicitarReenvioAnttSefaz")
            .routeId("rota-antt-reenvio-ondemand")
            .log("Disparando reenvio RNTRC sob demanda ANTT/SEFAZ. Placa: ${header.placa}, CNPJ: ${header.cnpj}")
            .bean("rntrcClient", "solicitarReenvioOnDemand(${header.placa}, ${header.cnpj})")
            .log("Resposta ANTT RNTRC: Status ${body.cStat} - ${body.xMotivo}");
    }
}
