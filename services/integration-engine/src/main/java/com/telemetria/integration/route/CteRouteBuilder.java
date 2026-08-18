package com.telemetria.integration.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CteRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Tratamento global de erros da rota
        onException(Exception.class)
            .handled(true)
            .to("log:erroCte?level=ERROR")
            .to("direct:tratarErroCte");

        // Rota de fallback para tratar falhas/erros de integração
        from("direct:tratarErroCte")
            .routeId("rota-tratamento-erro")
            .log("Tratando falha no processamento do CT-e: ${exception.message}")
            .setBody(simple("Erro no processamento do CT-e: ${exception.message}"));

        // Rota 1: Envio de CT-e individual/direto
        from("direct:enviarCte")
            .routeId("rota-envio-cte")
            .process("cteValidationProcessor") // Corrigido de .processor() para .process()
            .bean("cteClient", "autorizarCte")   // Chama o método autorizarCte do CteClient
            .to("log:cteEnviado?level=INFO");

        // Rota 2: Processamento em Lote (arquivos na pasta de entrada)
        from("file:data/cte/entrada?move=.processados&moveFailed=.erros")
            .routeId("rota-lote-cte")
            .split(xpath("//CTe")).streaming()  // Divide arquivos com múltiplos CT-es mantendo streaming em memória
                .process("cteValidationProcessor")
                .bean("cteClient", "autorizarCte")
                .to("direct:salvarResultadoBanco")
            .end();

        // Rota mock para salvar resultados no banco de dados
        from("direct:salvarResultadoBanco")
            .routeId("rota-salvar-banco")
            .log("Salvando retorno do CT-e no banco de dados...");
    }
}