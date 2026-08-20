package com.telemetria.integration.sefaz.cte;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Orquestrador para o ciclo de vida do CT-e (v4.00):
 * Emissão (Individual e Lote), Consultas (SEFAZ e Infosimples),
 * Eventos (Cancelamento) e Tratamento Global de Exceções.
 */
@Component
@ConditionalOnProperty(name = "integration.experimental-routes.enabled", havingValue = "true")
public class CteRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // =========================================================================
        // 1. TRATAMENTO GLOBAL DE EXCEÇÕES E FALLBACK
        // =========================================================================
        onException(Exception.class)
            .handled(true)
            .to("log:erroCte?level=ERROR&showCaughtException=true")
            .to("direct:tratarErroCte");

        from("direct:tratarErroCte")
            .routeId("rota-tratamento-erro")
            .log("Tratando falha no processamento do CT-e: ${exception.message}")
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .process(exchange -> {
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String msg = (cause != null && cause.getMessage() != null) ? cause.getMessage() : "Erro interno de integração";

                // Retorna estrutura Map para o Jackson serializar em JSON seguro (evita erros com aspas e caracteres especiais no XML)
                exchange.getIn().setBody(Map.of(
                    "status", "ERRO",
                    "etapa", "INTEGRACAO_CTE",
                    "mensagem", msg
                ));
            });

        // =========================================================================
        // 2. EMISSÃO DE CT-E (INDIVIDUAL E EM LOTE)
        // =========================================================================

        // Rota 1: Envio individual/direto de CT-e
        from("direct:enviarCte")
            .routeId("rota-envio-cte")
            .log("Iniciando validação e emissão direta de CT-e...")
            .bean("cteXmlValidator", "validarEstrutura")
            .bean("cteClient", "autorizarCte")
            .to("log:cteEnviado?level=INFO");

        // Rota 2: Processamento em Lote (Lê apenas XMLs no diretório de entrada)
        from("file:data/cte/entrada?include=.*\\.xml$&move=.processados&moveFailed=.erros")
            .routeId("rota-lote-cte")
            .log("Arquivo em lote detectado: ${file:name}")
            // Utiliza local-name() no XPath para garantir compatibilidade com o Namespace oficial CT-e (v4.00)
            .split(xpath("//*[local-name()='CTe']")).streaming()
                .bean("cteXmlValidator", "validarEstrutura")
                .bean("cteClient", "autorizarCte")
                .to("direct:salvarResultadoBanco")
            .end();

        // Rota para salvar resultados no banco de dados
        from("direct:salvarResultadoBanco")
            .routeId("rota-salvar-banco")
            .log("Salvando retorno do CT-e no banco de dados...");

        // =========================================================================
        // 3. CONSULTAS (SEFAZ SOAP vs REST INFOSIMPLES)
        // =========================================================================

        // Rota 3: Consulta leve de situação fiscal na SEFAZ (SOAP)
        from("direct:consultarSituacaoSefaz")
            .routeId("rota-consulta-sefaz-cte")
            .log("Consultando situação do CT-e na SEFAZ. Chave: ${body}")
            .bean("cteConsultaService", "consultarSituacaoSefaz")
            .bean("cteResponseParser", "parseRetorno")
            .to("log:resultadoConsultaSefaz?level=INFO");

        // Rota 4: Consulta completa/enriquecida e download de XML (Infosimples REST)
        from("direct:consultarDadosCompletosCte")
            .routeId("rota-consulta-completa-cte")
            .log("Consultando dados completos do CT-e via Infosimples. Chave: ${header.chaveCte}")
            .bean("cteConsultaService", "consultarDadosCompletosInfosimples(${header.chaveCte}, ${header.certBase64}, ${header.certPass})")
            .log("Consulta completa concluída com sucesso para o CT-e ${header.chaveCte}");

        // =========================================================================
        // 4. EVENTOS (CANCELAMENTO)
        // =========================================================================

        // Rota 5: Cancelamento de CT-e
        from("direct:cancelarCte")
            .routeId("rota-cancelamento-cte")
            .log("Iniciando cancelamento do CT-e chave: ${header.chaveCte}")
            .bean("cteEventoService", "cancelarCte(${header.chaveCte}, ${header.nProt}, ${header.xJust}, ${header.cnpjEmissor})")
            .log("Evento de cancelamento processado junto à SEFAZ.");
    }
}