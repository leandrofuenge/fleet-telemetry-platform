package com.telemetria.api.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.telemetria.application.service.BackpressureMonitorService;

@ExtendWith(MockitoExtension.class)
class BackpressureAdminControllerTest {

    @Mock
    private BackpressureMonitorService backpressureMonitor;

    @InjectMocks
    private BackpressureAdminController backpressureAdminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(backpressureAdminController)
                .build();
    }

    @Nested
    class Status {

        @Test
        @DisplayName("Deve retornar status do backpressure com sucesso")
        void deveRetornarStatusDoBackpressureComSucesso() throws Exception {
            when(backpressureMonitor.calcularLag()).thenReturn(25);
            when(backpressureMonitor.calcularTaxaProcessamento()).thenReturn(120.5);
            when(backpressureMonitor.getCpuUsage()).thenReturn(63.2);
            when(backpressureMonitor.getMemoryUsage()).thenReturn(71.8);
            when(backpressureMonitor.isBackpressureAtivo()).thenReturn(true);
            when(backpressureMonitor.getMensagensRecebidas()).thenReturn(1500);
            when(backpressureMonitor.getMensagensProcessadas()).thenReturn(1475);
            when(backpressureMonitor.getTempoTotalProcessamento()).thenReturn(9876L);

            mockMvc.perform(get("/api/v1/admin/backpressure/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lag").value(25))
                    .andExpect(jsonPath("$.taxaProcessamento").value(120.5))
                    .andExpect(jsonPath("$.cpuUsage").value(63.2))
                    .andExpect(jsonPath("$.memoryUsage").value(71.8))
                    .andExpect(jsonPath("$.backpressureAtivo").value(true))
                    .andExpect(jsonPath("$.mensagensRecebidas").value(1500))
                    .andExpect(jsonPath("$.mensagensProcessadas").value(1475))
                    .andExpect(jsonPath("$.tempoTotalProcessamento").value(9876));

            verify(backpressureMonitor).calcularLag();
            verify(backpressureMonitor).calcularTaxaProcessamento();
            verify(backpressureMonitor).getCpuUsage();
            verify(backpressureMonitor).getMemoryUsage();
            verify(backpressureMonitor).isBackpressureAtivo();
            verify(backpressureMonitor).getMensagensRecebidas();
            verify(backpressureMonitor).getMensagensProcessadas();
            verify(backpressureMonitor).getTempoTotalProcessamento();
        }
    }

    @Nested
    class Configuracao {

        @Test
        @DisplayName("Deve atualizar todas as configurações com sucesso")
        void deveAtualizarTodasAsConfiguracoesComSucesso() throws Exception {
            when(backpressureMonitor.getLagThreshold()).thenReturn(100);
            when(backpressureMonitor.getCpuThreshold()).thenReturn(80);
            when(backpressureMonitor.getMemoryThreshold()).thenReturn(85);
            when(backpressureMonitor.getPauseDurationMs()).thenReturn(5000);

            mockMvc.perform(post("/api/v1/admin/backpressure/config")
                            .param("lagThreshold", "200")
                            .param("cpuThreshold", "75")
                            .param("memoryThreshold", "70")
                            .param("pauseDuration", "3000"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Configurações atualizadas"));

            verify(backpressureMonitor).setLagThreshold(200);
            verify(backpressureMonitor).setCpuThreshold(75);
            verify(backpressureMonitor).setMemoryThreshold(70);
            verify(backpressureMonitor).setPauseDurationMs(3000);

            verify(backpressureMonitor, times(2)).getLagThreshold();
            verify(backpressureMonitor, times(2)).getCpuThreshold();
            verify(backpressureMonitor, times(2)).getMemoryThreshold();
            verify(backpressureMonitor, times(2)).getPauseDurationMs();
        }

        @Test
        @DisplayName("Deve atualizar apenas lagThreshold")
        void deveAtualizarApenasLagThreshold() throws Exception {
            when(backpressureMonitor.getLagThreshold()).thenReturn(100);
            when(backpressureMonitor.getCpuThreshold()).thenReturn(80);
            when(backpressureMonitor.getMemoryThreshold()).thenReturn(85);
            when(backpressureMonitor.getPauseDurationMs()).thenReturn(5000);

            mockMvc.perform(post("/api/v1/admin/backpressure/config")
                            .param("lagThreshold", "150"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Configurações atualizadas"));

            verify(backpressureMonitor).setLagThreshold(150);

            verify(backpressureMonitor, times(2)).getLagThreshold();
            verify(backpressureMonitor).getCpuThreshold();
            verify(backpressureMonitor).getMemoryThreshold();
            verify(backpressureMonitor).getPauseDurationMs();
        }

        @Test
        @DisplayName("Deve atualizar apenas cpuThreshold")
        void deveAtualizarApenasCpuThreshold() throws Exception {
            when(backpressureMonitor.getCpuThreshold()).thenReturn(80);
            when(backpressureMonitor.getLagThreshold()).thenReturn(100);
            when(backpressureMonitor.getMemoryThreshold()).thenReturn(85);
            when(backpressureMonitor.getPauseDurationMs()).thenReturn(5000);

            mockMvc.perform(post("/api/v1/admin/backpressure/config")
                            .param("cpuThreshold", "65"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Configurações atualizadas"));

            verify(backpressureMonitor).setCpuThreshold(65);

            verify(backpressureMonitor).getLagThreshold();
            verify(backpressureMonitor, times(2)).getCpuThreshold();
            verify(backpressureMonitor).getMemoryThreshold();
            verify(backpressureMonitor).getPauseDurationMs();
        }

        @Test
        @DisplayName("Deve atualizar apenas memoryThreshold")
        void deveAtualizarApenasMemoryThreshold() throws Exception {
            when(backpressureMonitor.getMemoryThreshold()).thenReturn(85);
            when(backpressureMonitor.getLagThreshold()).thenReturn(100);
            when(backpressureMonitor.getCpuThreshold()).thenReturn(80);
            when(backpressureMonitor.getPauseDurationMs()).thenReturn(5000);

            mockMvc.perform(post("/api/v1/admin/backpressure/config")
                            .param("memoryThreshold", "60"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Configurações atualizadas"));

            verify(backpressureMonitor).setMemoryThreshold(60);

            verify(backpressureMonitor).getLagThreshold();
            verify(backpressureMonitor).getCpuThreshold();
            verify(backpressureMonitor, times(2)).getMemoryThreshold();
            verify(backpressureMonitor).getPauseDurationMs();
        }

        @Test
        @DisplayName("Deve atualizar apenas pauseDuration")
        void deveAtualizarApenasPauseDuration() throws Exception {
            when(backpressureMonitor.getPauseDurationMs()).thenReturn(5000);
            when(backpressureMonitor.getLagThreshold()).thenReturn(100);
            when(backpressureMonitor.getCpuThreshold()).thenReturn(80);
            when(backpressureMonitor.getMemoryThreshold()).thenReturn(85);

            mockMvc.perform(post("/api/v1/admin/backpressure/config")
                            .param("pauseDuration", "2000"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Configurações atualizadas"));

            verify(backpressureMonitor).setPauseDurationMs(2000);

            verify(backpressureMonitor).getLagThreshold();
            verify(backpressureMonitor).getCpuThreshold();
            verify(backpressureMonitor).getMemoryThreshold();
            verify(backpressureMonitor, times(2)).getPauseDurationMs();
        }

        @Test
        @DisplayName("Deve retornar sucesso mesmo sem parâmetros")
        void deveRetornarSucessoMesmoSemParametros() throws Exception {
            when(backpressureMonitor.getLagThreshold()).thenReturn(100);
            when(backpressureMonitor.getCpuThreshold()).thenReturn(80);
            when(backpressureMonitor.getMemoryThreshold()).thenReturn(85);
            when(backpressureMonitor.getPauseDurationMs()).thenReturn(5000);

            mockMvc.perform(post("/api/v1/admin/backpressure/config"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Configurações atualizadas"));

            verify(backpressureMonitor).getLagThreshold();
            verify(backpressureMonitor).getCpuThreshold();
            verify(backpressureMonitor).getMemoryThreshold();
            verify(backpressureMonitor).getPauseDurationMs();
            verifyNoMoreInteractions(backpressureMonitor);
        }
    }
}