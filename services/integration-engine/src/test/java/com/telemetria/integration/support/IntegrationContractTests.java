package com.telemetria.integration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class IntegrationContractTests {

    @Test
    void requestRequiresOperationAndCopiesData() {
        assertThrows(IllegalArgumentException.class, () -> new IntegrationRequest(" ", null));
        IntegrationRequest request = new IntegrationRequest("senatran.renavam.consultar", Map.of("renavam", "1"));
        assertEquals("1", request.data().get("renavam"));
    }

    @Test
    void clientRejectsMissingConfigurationBeforeNetworkCall() {
        TestClient client = new TestClient();
        IntegrationRequest request = new IntegrationRequest("test.execute", Map.of());
        assertThrows(IllegalStateException.class, () -> client.call("", "token", request));
        assertThrows(IllegalStateException.class, () -> client.call("https://example.invalid", "", request));
    }

    private static final class TestClient extends ConfigurableIntegrationClient {
        private TestClient() { super(new RestTemplate()); }
        private IntegrationResponse call(String endpoint, String token, IntegrationRequest request) {
            return post(endpoint, token, request);
        }
    }
}
