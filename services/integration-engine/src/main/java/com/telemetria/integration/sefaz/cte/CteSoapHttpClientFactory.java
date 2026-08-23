package com.telemetria.integration.sefaz.cte;

import java.net.http.HttpClient;

import javax.net.ssl.SSLContext;

public final class CteSoapHttpClientFactory {
    public HttpClient create(SSLContext sslContext, CteSoapProperties properties) {
        return HttpClient.newBuilder().sslContext(sslContext)
                .connectTimeout(properties.connectTimeout())
                .version(HttpClient.Version.HTTP_1_1).build();
    }
}
