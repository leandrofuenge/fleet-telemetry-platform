package com.telemetria.integration.senatran.serpro;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SerproEndpointTests {
    private SerproConsultaService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(SerproConsultaService.class);
        SerproProperties properties = new SerproProperties();
        properties.getSerpro().setApiKey("chave-interna");
        mvc = MockMvcBuilders.standaloneSetup(new SerproConsultaController(service))
                .setControllerAdvice(new SerproExceptionHandler())
                .addFilters(new SerproCorrelationFilter(), new SerproApiKeyFilter(properties)).build();
    }

    @Test
    void exigeApiKey() throws Exception {
        mvc.perform(post("/api/integracoes/senatran/serpro/veiculos/consulta")
                .contentType(MediaType.APPLICATION_JSON).content("{\"placa\":\"ABC1D23\",\"renavam\":\"12345678900\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retornaConsultaComCorrelacao() throws Exception {
        when(service.consultarVeiculo("ABC1D23", "12345678900"))
                .thenReturn(new SerproVeiculoResponse(200, "OK", List.of(), List.of(), true, null));
        mvc.perform(post("/api/integracoes/senatran/serpro/veiculos/consulta")
                .header(SerproApiKeyFilter.HEADER, "chave-interna")
                .header(SerproCorrelationFilter.HEADER, "corr-123")
                .contentType(MediaType.APPLICATION_JSON).content("{\"placa\":\"ABC1D23\",\"renavam\":\"12345678900\"}"))
                .andExpect(status().isOk()).andExpect(header().string(SerproCorrelationFilter.HEADER, "corr-123"))
                .andExpect(jsonPath("$.sucesso").value(true));
    }

    @Test
    void converteValidacaoEmBadRequest() throws Exception {
        when(service.consultarVeiculo("INVALIDA", "1"))
                .thenThrow(new InvalidVehicleQueryException("Dados inválidos"));
        mvc.perform(post("/api/integracoes/senatran/serpro/veiculos/consulta")
                .header(SerproApiKeyFilter.HEADER, "chave-interna")
                .contentType(MediaType.APPLICATION_JSON).content("{\"placa\":\"INVALIDA\",\"renavam\":\"1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_VEHICLE_QUERY"));
    }

    @Test
    void converteIndisponibilidadeEmServiceUnavailable() throws Exception {
        when(service.consultarVeiculo("ABC1D23", "12345678900"))
                .thenThrow(new SerproIntegrationException("Fornecedor indisponível"));
        mvc.perform(post("/api/integracoes/senatran/serpro/veiculos/consulta")
                .header(SerproApiKeyFilter.HEADER, "chave-interna")
                .contentType(MediaType.APPLICATION_JSON).content("{\"placa\":\"ABC1D23\",\"renavam\":\"12345678900\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SERPRO_UNAVAILABLE"));
    }
}
