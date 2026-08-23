package com.telemetria.integration.sefaz.cte;

import java.util.Optional;

public final class CteSoapFaultParser {
    public Optional<CteSoapFault> parse(String xml) {
        if (xml == null || (!xml.contains(":Fault") && !xml.contains("<Fault"))) return Optional.empty();
        return Optional.of(new CteSoapFault("SOAP-FAULT", "A SEFAZ retornou um SOAP Fault.", xml));
    }
}
