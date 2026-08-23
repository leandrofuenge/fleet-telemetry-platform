package com.telemetria.integration.sefaz.cte;

public interface CteSoapInterceptor {
    default CteSoapRequest before(CteSoapRequest request) { return request; }
    default void after(CteSoapRequest request, CteSoapResponse response) { }
    default void onError(CteSoapRequest request, RuntimeException exception) { }
}
