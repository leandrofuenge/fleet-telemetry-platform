package com.telemetria.integration.sefaz.cte;

public final class CteSoapEnvelopeBuilder {
    private final CteSoapHeaderBuilder headerBuilder;
    public CteSoapEnvelopeBuilder(CteSoapHeaderBuilder headerBuilder) { this.headerBuilder = headerBuilder; }
    public String build(String xmlBody) {
        if (xmlBody == null || xmlBody.isBlank()) throw new IllegalArgumentException("Corpo SOAP é obrigatório.");
        return "<soap:Envelope xmlns:soap=\"" + CteSoapNamespaces.SOAP_12 + "\">"
                + headerBuilder.build() + "<soap:Body>" + xmlBody + "</soap:Body></soap:Envelope>";
    }
}
