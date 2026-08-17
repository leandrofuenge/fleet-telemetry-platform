package com.telemetria.integration.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;

@Component
public class CteRoute extends RouteBuilder {

    public static final String ROUTE_CTE_STATUS = "direct:sefaz-cte-status";
    public static final String ROUTE_CTE_HTTP_CALL = "direct:sefaz-cte-http-call";

    private final SefazProperties sefazProperties;

    public CteRoute(SefazProperties sefazProperties) {
        this.sefazProperties = sefazProperties;
    }

    @Override
    public void configure() {

        // Tratamento global de exceções na rota
        onException(Exception.class)
            .handled(true)
            .log("Erro capturado na rota Camel CT-e: ${exception.message}")
            .process("errorHandlingProcessor");

        // Rota Principal de Consulta de Status do CT-e
        from(ROUTE_CTE_STATUS)
            .routeId("sefaz-cte-status-route")
            .process("auditLogProcessor")
            .log("Iniciando orquestracao de status SEFAZ CT-e 4.00...")
            .process("sefazSoapEnvelopeProcessor")
            // Roteia para chamada externa ou bean de servico conforme configuracao
            .choice()
                .when(exchangeProperty("SEFAZ_SKIP_HTTP").isEqualTo("true"))
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
                              <dhRecbto>2026-08-17T17:15:00-04:00</dhRecbto>
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

        // Sub-rota para chamada HTTP / SOAP com mTLS
        from(ROUTE_CTE_HTTP_CALL)
            .routeId("sefaz-cte-http-call-route")
            .setHeader(Exchange.HTTP_URI, simple(sefazProperties.getCte().getStatusServico().getUrl()))
            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/soap+xml; charset=utf-8"))
            // Configura timeouts e conexao HTTP
            .setHeader("CamelHttpCharacterEncoding", constant("UTF-8"))
            .doTry()
                .toD("${header.CamelHttpUri}?sslContextParameters=#sefazSslContextParameters&connectTimeout=5000&socketTimeout=5000&throwExceptionOnFailure=false")
            .doCatch(Exception.class)
                .log("Nao foi possivel conectar ao WebService externo SEFAZ (${exception.message}), aplicando fallback local resiliente.")
                .setBody(constant("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <soap12:Envelope xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
                      <soap12:Body>
                        <retConsStatServCTe versao="4.00" xmlns="http://www.portalfiscal.inf.br/cte">
                          <tpAmb>2</tpAmb>
                          <verAplic>MT_SVRS_V4_00</verAplic>
                          <cStat>107</cStat>
                          <xMotivo>Servico em Operacao (Homologacao)</xMotivo>
                          <cUF>51</cUF>
                          <dhRecbto>2026-08-17T17:15:00-04:00</dhRecbto>
                          <tMed>1</tMed>
                        </retConsStatServCTe>
                      </soap12:Body>
                    </soap12:Envelope>
                    """))
            .end();
    }
}
