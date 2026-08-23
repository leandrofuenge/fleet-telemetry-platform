package com.telemetria.integration.sefaz.cte;

public final class CteResponseParser {
    private final CteSoapFaultParser faultParser;
    public CteResponseParser(CteSoapFaultParser faultParser) { this.faultParser = faultParser; }
    public String parse(String xml) {
        if (xml == null || xml.isBlank()) throw new CteParseException("Resposta SOAP vazia.", null);
        faultParser.parse(xml).ifPresent(fault -> { throw new CteSoapFaultException(fault); });
        return xml;
    }
}
