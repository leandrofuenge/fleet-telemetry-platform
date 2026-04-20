package com.app.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.service.TelemetriaService;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

@ExtendWith(MockitoExtension.class)
class TelemetriaServiceTest {

    @Mock
    private TelemetriaRepository telemetriaRepository;

    @InjectMocks
    private TelemetriaService telemetriaService;

    private Telemetria telemetria;

    @BeforeEach
    void setup() {
        telemetria = new Telemetria();
        telemetria.setId(1L);
        telemetria.setVeiculoId(100L);
        telemetria.setLatitude(-15.601);
        telemetria.setLongitude(-56.097);
        telemetria.setVelocidade(82.5);
        telemetria.setDataHora(LocalDateTime.now());
    }

    @Nested
    class BuscarUltimaPorVeiculo {

        @Test
        @DisplayName("Deve buscar última telemetria por veículo")
        void deveBuscarUltimaPorVeiculo() {
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.of(telemetria));

            Optional<Telemetria> resultado = telemetriaService.buscarUltimaPorVeiculo(100L);

            assertTrue(resultado.isPresent());
            assertEquals(1L, resultado.get().getId());
            assertEquals(100L, resultado.get().getVeiculoId());
            verify(telemetriaRepository).findUltimaTelemetriaByVeiculoId(100L);
        }

        @Test
        @DisplayName("Deve retornar vazio quando não houver telemetria do veículo")
        void deveRetornarVazioQuandoNaoHouverTelemetria() {
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.empty());

            Optional<Telemetria> resultado = telemetriaService.buscarUltimaPorVeiculo(100L);

            assertTrue(resultado.isEmpty());
            verify(telemetriaRepository).findUltimaTelemetriaByVeiculoId(100L);
        }
    }

    @Nested
    class ListarPorVeiculo {

        @Test
        @DisplayName("Deve listar telemetrias por veículo ordenadas por data")
        void deveListarPorVeiculo() {
            Telemetria t2 = new Telemetria();
            t2.setId(2L);
            t2.setVeiculoId(100L);
            t2.setDataHora(LocalDateTime.now().minusMinutes(5));

            when(telemetriaRepository.findByVeiculoIdOrderByDataHoraDesc(100L))
                    .thenReturn(List.of(telemetria, t2));

            List<Telemetria> resultado = telemetriaService.listarPorVeiculo(100L);

            assertNotNull(resultado);
            assertEquals(2, resultado.size());
            assertEquals(100L, resultado.get(0).getVeiculoId());
            verify(telemetriaRepository).findByVeiculoIdOrderByDataHoraDesc(100L);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver telemetrias do veículo")
        void deveRetornarListaVaziaQuandoNaoHouverTelemetrias() {
            when(telemetriaRepository.findByVeiculoIdOrderByDataHoraDesc(100L))
                    .thenReturn(List.of());

            List<Telemetria> resultado = telemetriaService.listarPorVeiculo(100L);

            assertNotNull(resultado);
            assertTrue(resultado.isEmpty());
            verify(telemetriaRepository).findByVeiculoIdOrderByDataHoraDesc(100L);
        }
    }

    @Nested
    class ListarPorPeriodo {

        @Test
        @DisplayName("Deve listar telemetrias por veículo e período")
        void deveListarPorPeriodo() {
            LocalDateTime inicio = LocalDateTime.now().minusHours(2);
            LocalDateTime fim = LocalDateTime.now();

            Telemetria t2 = new Telemetria();
            t2.setId(2L);
            t2.setVeiculoId(100L);
            t2.setDataHora(LocalDateTime.now().minusMinutes(30));

            when(telemetriaRepository.findByVeiculoIdAndDataHoraBetween(100L, inicio, fim))
                    .thenReturn(List.of(telemetria, t2));

            List<Telemetria> resultado = telemetriaService.listarPorPeriodo(100L, inicio, fim);

            assertNotNull(resultado);
            assertEquals(2, resultado.size());
            verify(telemetriaRepository).findByVeiculoIdAndDataHoraBetween(100L, inicio, fim);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver telemetrias no período")
        void deveRetornarListaVaziaQuandoNaoHouverTelemetriasNoPeriodo() {
            LocalDateTime inicio = LocalDateTime.now().minusHours(2);
            LocalDateTime fim = LocalDateTime.now();

            when(telemetriaRepository.findByVeiculoIdAndDataHoraBetween(100L, inicio, fim))
                    .thenReturn(List.of());

            List<Telemetria> resultado = telemetriaService.listarPorPeriodo(100L, inicio, fim);

            assertNotNull(resultado);
            assertTrue(resultado.isEmpty());
            verify(telemetriaRepository).findByVeiculoIdAndDataHoraBetween(100L, inicio, fim);
        }
    }
}