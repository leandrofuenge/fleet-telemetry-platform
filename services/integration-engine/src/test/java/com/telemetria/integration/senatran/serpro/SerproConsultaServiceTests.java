package com.telemetria.integration.senatran.serpro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SerproConsultaServiceTests {
    private SerproConsultaClient client;
    private SerproProperties properties;
    private SerproConsultaService service;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(SerproConsultaClient.class);
        properties = new SerproProperties();
        service = new SerproConsultaService(client, properties, new SerproConsultaCache(properties));
    }

    @Test
    void normalizaEConsultaPlacaMercosulERenavamValido() {
        when(client.consultarVeiculo(Mockito.any())).thenReturn(success(List.of(vehicle(List.of()))));
        service.consultarVeiculo("abc-1d23", "123.456.789-00");
        ArgumentCaptor<SerproVeiculoRequest> captor = ArgumentCaptor.forClass(SerproVeiculoRequest.class);
        verify(client).consultarVeiculo(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new SerproVeiculoRequest("ABC1D23", "12345678900"));
    }

    @Test
    void rejeitaPlacaComFormatoGenerico() {
        assertThatThrownBy(() -> service.consultarVeiculo("1234567", "12345678900"))
                .isInstanceOf(InvalidVehicleQueryException.class).hasMessageContaining("Placa inválida");
    }

    @Test
    void rejeitaDigitoVerificadorDoRenavamInvalido() {
        assertThatThrownBy(() -> service.consultarVeiculo("ABC1D23", "12345678901"))
                .isInstanceOf(InvalidVehicleQueryException.class).hasMessageContaining("dígito verificador");
    }

    @Test
    void bloqueiaInfracaoPendente() {
        when(client.consultarVeiculo(Mockito.any())).thenReturn(success(List.of(vehicle(List.of(infraction("PENDENTE"))))));
        assertThat(service.isVeiculoAptoParaViagem("ABC1D23", "12345678900")).isFalse();
    }

    @Test
    void liberaQuandoTodasAsInfracoesEstaoEncerradas() {
        when(client.consultarVeiculo(Mockito.any())).thenReturn(success(List.of(vehicle(List.of(infraction("PAGO"))))));
        assertThat(service.isVeiculoAptoParaViagem("ABC1D23", "12345678900")).isTrue();
    }

    @Test
    void bloqueiaStatusDesconhecidoPorPadrao() {
        when(client.consultarVeiculo(Mockito.any())).thenReturn(success(List.of(vehicle(List.of(infraction("NOVO_STATUS"))))));
        assertThat(service.isVeiculoAptoParaViagem("ABC1D23", "12345678900")).isFalse();
    }

    @Test
    void bloqueiaRespostaSemVeiculo() {
        when(client.consultarVeiculo(Mockito.any())).thenReturn(success(List.of()));
        assertThat(service.isVeiculoAptoParaViagem("ABC1D23", "12345678900")).isFalse();
    }

    @Test
    void converteFalhaDoClienteEmExcecaoDoDominio() {
        when(client.consultarVeiculo(Mockito.any())).thenReturn(SerproVeiculoResponse.erro("indisponível"));
        assertThatThrownBy(() -> service.consultarVeiculo("ABC1D23", "12345678900"))
                .isInstanceOf(SerproIntegrationException.class).hasMessageContaining("indisponível");
    }

    private SerproVeiculoResponse success(List<SerproVeiculoResponse.VeiculoRadarData> data) {
        return new SerproVeiculoResponse(200, "OK", data, List.of(), true, null);
    }
    private SerproVeiculoResponse.VeiculoRadarData vehicle(List<SerproVeiculoResponse.InfracaoData> infractions) {
        return new SerproVeiculoResponse.VeiculoRadarData("ABC1D23", "MT", "MODELO", infractions);
    }
    private SerproVeiculoResponse.InfracaoData infraction(String status) {
        return new SerproVeiculoResponse.InfracaoData("1", "Teste", null, null, null, status, null, null, null);
    }
}
