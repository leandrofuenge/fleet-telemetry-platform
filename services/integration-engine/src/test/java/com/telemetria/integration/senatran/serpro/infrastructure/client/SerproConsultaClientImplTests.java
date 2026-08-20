package com.telemetria.integration.senatran.serpro.infrastructure.client;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoRequest;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoResponse;
import com.telemetria.integration.senatran.serpro.infrastructure.config.SerproProperties;
import com.telemetria.integration.senatran.serpro.infrastructure.resilience.SerproResiliencePolicy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class SerproConsultaClientImplTests {
    private SerproConsultaClientImpl client;
    private MockRestServiceServer server;
    private SerproProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SerproProperties();
        properties.setUrlSerproRadar("https://fornecedor.test/veiculo");
        properties.setToken("segredo");
        properties.getSerpro().setRetryDelay(Duration.ZERO);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        client = new SerproConsultaClientImpl(new RestTemplateBuilder(), properties, registry,
                new SerproResiliencePolicy(properties));
        RestTemplate template = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(template).build();
    }

    @Test
    void converteContratoExternoEmRespostaInterna() {
        server.expect(requestTo("https://fornecedor.test/veiculo?token=segredo"))
                .andRespond(withSuccess("""
                        {"code":200,"code_message":"OK","data":[{"placa":"ABC1D23","uf":"MT",
                        "marca_modelo":"MODELO","infracoes":[]}],"campo_novo":"ignorado"}
                        """, MediaType.APPLICATION_JSON));
        SerproVeiculoResponse response = client.consultarVeiculo(new SerproVeiculoRequest("ABC1D23", "12345678900"));
        assertThat(response.isSucesso()).isTrue();
        assertThat(response.data()).hasSize(1);
        server.verify();
    }

    @Test
    void trataCorpoVazio() {
        server.expect(requestTo("https://fornecedor.test/veiculo?token=segredo"))
                .andRespond(withSuccess());
        assertThat(client.consultarVeiculo(new SerproVeiculoRequest("ABC1D23", "12345678900")).isSucesso())
                .isFalse();
    }

    @Test
    void repeteFalhaTransitoriaAteObterSucesso() {
        server.expect(times(2), requestTo("https://fornecedor.test/veiculo?token=segredo"))
                .andRespond(withServerError());
        server.expect(requestTo("https://fornecedor.test/veiculo?token=segredo"))
                .andRespond(withSuccess("{\"code\":200,\"data\":[]}", MediaType.APPLICATION_JSON));
        assertThat(client.consultarVeiculo(new SerproVeiculoRequest("ABC1D23", "12345678900")).isSucesso())
                .isTrue();
        server.verify();
    }

    @Test
    void naoIncluiTokenNaMensagemDeErro() {
        properties.getSerpro().setMaxAttempts(1);
        server.expect(requestTo("https://fornecedor.test/veiculo?token=segredo"))
                .andRespond(withServerError());
        SerproVeiculoResponse response = client.consultarVeiculo(new SerproVeiculoRequest("ABC1D23", "12345678900"));
        assertThat(response.mensagemErro()).doesNotContain("segredo");
    }
}
