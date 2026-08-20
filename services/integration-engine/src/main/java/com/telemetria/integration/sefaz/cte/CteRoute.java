package com.telemetria.integration.sefaz.cte;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;

@Component
public class CteRoute extends RouteBuilder {

    public static final String ROUTE_CTE_STATUS = "direct:sefaz-cte-status";
    public static final String ROUTE_CTE_HTTP_CALL = "direct:sefaz-cte-http-call";

    private final SefazProperties sefazProperties;

    @Value("${integration.simulation.enabled:false}")
    private boolean simulationEnabled;

    public CteRoute(SefazProperties sefazProperties) {
        this.sefazProperties = sefazProperties;
    }

    @Override
    public void configure() {

        // 1. Tratamento Global de Exceções na Rota
        onException(Exception.class)
            .handled(true)
            .log("Erro capturado na rota Camel CT-e: ${exception.message}")
            .process("errorHandlingProcessor");

        // 2. Rota Principal de Consulta de Status do CT-e
        from(ROUTE_CTE_STATUS)
            .routeId("sefaz-cte-status-route")
            .process("auditLogProcessor")
            .log("Iniciando orquestração de status SEFAZ CT-e 4.00...")
            .process("sefazSoapEnvelopeProcessor")
            .setProperty("SEFAZ_SKIP_HTTP", constant(simulationEnabled))
            
            // Roteamento condicional (Simulação vs Chamada Real SEFAZ)
            .choice()
                .when(exchangeProperty("SEFAZ_SKIP_HTTP").isEqualTo(true))
                    .log("Modo simulado ativo para SEFAZ CT-e")
                    .setBody(constant("""
                        <?xml version="1.0" encoding="utf-8"?>
                        <soap12:Envelope xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
                          <soap12:Body>
                            <retConsStatServCTe versao="4.00" xmlns="http://www.portalfiscal.inf.br/cte">
                              <tpAmb>2</tpAmb>
                              <verAplic>RS2026_V4_00</verAplic>
                              <cStat>107</cStat>
                              <xMotivo>Servico em Operacao</xMotivo>
                              <cUF>51</cUF>
                              <dhRecbto>2026-08-20T10:00:00-04:00</dhRecbto>
                              <tMed>1</tMed>
                            </retConsStatServCTe>
                          </soap12:Body>
                        </soap12:Envelope>
                        """))
                .otherwise()
                    .to(ROUTE_CTE_HTTP_CALL)
            .end()
            .process("sefazResponseParserProcessor")
            .process("auditLogProcessor")
            .log("Consulta de status CT-e finalizada com status: ${body.codigo} - ${body.mensagem}");

        // 3. Sub-rota para Chamada HTTP / SOAP Externa
        from(ROUTE_CTE_HTTP_CALL)
            .routeId("sefaz-cte-http-call-route")
            .process(this::prepararEndpointSefaz)
            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
            .setHeader("CamelHttpCharacterEncoding", constant("UTF-8"))
            .doTry()
                // bridgeEndpoint=true garante que o Camel preserve a URI definida no header Exchange.HTTP_URI
                .toD("${header.SEFAZ_ENDPOINT_URL}?sslContextParameters=#sefazSslContextParameters&connectTimeout=5000&socketTimeout=5000&throwExceptionOnFailure=false&bridgeEndpoint=true")
            .doCatch(Exception.class)
                .process(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    throw new CteException("Falha na comunicação HTTPS/SOAP com a SEFAZ: " + cause.getMessage(), cause);
                })
            .end();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void prepararEndpointSefaz(Exchange exchange) {
        URI endpointUri = Optional.ofNullable(sefazProperties.getCte())
                .map(SefazProperties.Cte::getEndpoints)
                .map(SefazProperties.Endpoints::getStatus)
                .orElseThrow(() -> new CteException("Endpoint da SEFAZ para consulta de Status do CT-e não foi configurado em 'sefaz.cte.endpoints.status'."));

        String endpointStr = endpointUri.toString();
        exchange.getIn().setHeader("SEFAZ_ENDPOINT_URL", endpointStr);
        exchange.getIn().setHeader(Exchange.HTTP_URI, endpointStr);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, 
                "application/soap+xml; charset=utf-8; action=\"" + CteSoapService.STATUS.soapAction() + "\"");
    }
}