package com.app.telemetria.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.app.telemetria.domain.entity.Alerta;
import com.app.telemetria.domain.enums.SeveridadeAlerta;
import com.app.telemetria.domain.enums.TipoAlerta;
import com.app.telemetria.domain.service.AlertaService;

@ExtendWith(MockitoExtension.class)
class AlertaControllerTest {

    @Mock
    private AlertaService alertaService;

    @InjectMocks
    private AlertaController alertaController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(alertaController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private Alerta criarAlerta(Long id, TipoAlerta tipo, SeveridadeAlerta severidade, String mensagem) {
        Alerta alerta = new Alerta();
        alerta.setId(id);
        alerta.setTenantId(10L);
        alerta.setVeiculoId(100L);
        alerta.setMotoristaId(200L);
        alerta.setViagemId(300L);
        alerta.setTipo(tipo);
        alerta.setSeveridade(severidade);
        alerta.setMensagem(mensagem);
        alerta.setLatitude(-15.60);
        alerta.setLongitude(-56.10);
        alerta.setVelocidadeKmh(120.0);
        alerta.setOdometroKm(15000.0);
        alerta.setDataHora(LocalDateTime.of(2026, 3, 30, 10, 0, 0));
        alerta.setLido(false);
        alerta.setResolvido(false);
        return alerta;
    }

    @Nested
    class Consultas {

        @Test
        @DisplayName("Deve listar todos os alertas paginados")
        void deveListarTodos() throws Exception {
            Alerta alerta1 = criarAlerta(
                    1L,
                    TipoAlerta.EXCESSO_VELOCIDADE,
                    SeveridadeAlerta.ALTO,
                    "Excesso de velocidade"
            );
            Alerta alerta2 = criarAlerta(
                    2L,
                    TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO,
                    SeveridadeAlerta.MEDIO,
                    "Combustível baixo"
            );

            Page<Alerta> page = new PageImpl<>(
                    List.of(alerta1, alerta2),
                    PageRequest.of(0, 10),
                    2
            );

            when(alertaService.listarTodos(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/alertas")
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[1].id").value(2));

            verify(alertaService).listarTodos(any());
        }

        @Test
        @DisplayName("Deve listar alertas ativos")
        void deveListarAtivos() throws Exception {
            Alerta alerta1 = criarAlerta(
                    1L,
                    TipoAlerta.EXCESSO_VELOCIDADE,
                    SeveridadeAlerta.ALTO,
                    "Excesso de velocidade"
            );
            Alerta alerta2 = criarAlerta(
                    2L,
                    TipoAlerta.GPS_SEM_SINAL,
                    SeveridadeAlerta.ALTO,
                    "GPS sem sinal"
            );

            when(alertaService.listarAtivos()).thenReturn(List.of(alerta1, alerta2));

            mockMvc.perform(get("/api/v1/alertas/ativos")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(2));

            verify(alertaService).listarAtivos();
        }

        @Test
        @DisplayName("Deve listar alertas por veículo")
        void deveListarPorVeiculo() throws Exception {
            Alerta alerta = criarAlerta(
                    1L,
                    TipoAlerta.EXCESSO_VELOCIDADE,
                    SeveridadeAlerta.ALTO,
                    "Excesso de velocidade"
            );

            when(alertaService.listarPorVeiculo(100L)).thenReturn(List.of(alerta));

            mockMvc.perform(get("/api/v1/alertas/veiculo/100")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1));

            verify(alertaService).listarPorVeiculo(100L);
        }

        @Test
        @DisplayName("Deve listar alertas por motorista")
        void deveListarPorMotorista() throws Exception {
            Alerta alerta = criarAlerta(
                    1L,
                    TipoAlerta.TEMPO_DIRECAO,
                    SeveridadeAlerta.ALTO,
                    "Tempo de direção excedido"
            );

            when(alertaService.listarPorMotorista(200L)).thenReturn(List.of(alerta));

            mockMvc.perform(get("/api/v1/alertas/motorista/200")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1));

            verify(alertaService).listarPorMotorista(200L);
        }

        @Test
        @DisplayName("Deve listar alertas por viagem")
        void deveListarPorViagem() throws Exception {
            Alerta alerta = criarAlerta(
                    1L,
                    TipoAlerta.ATRASO_VIAGEM,
                    SeveridadeAlerta.ALTO,
                    "Atraso na viagem"
            );

            when(alertaService.listarPorViagem(300L)).thenReturn(List.of(alerta));

            mockMvc.perform(get("/api/v1/alertas/viagem/300")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1));

            verify(alertaService).listarPorViagem(300L);
        }

        @Test
        @DisplayName("Deve listar alertas por período")
        void deveListarPorPeriodo() throws Exception {
            Alerta alerta = criarAlerta(
                    1L,
                    TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO,
                    SeveridadeAlerta.MEDIO,
                    "Combustível baixo"
            );

            LocalDateTime inicio = LocalDateTime.of(2026, 3, 1, 0, 0, 0);
            LocalDateTime fim = LocalDateTime.of(2026, 3, 31, 23, 59, 59);

            when(alertaService.listarPorPeriodo(inicio, fim)).thenReturn(List.of(alerta));

            mockMvc.perform(get("/api/v1/alertas/periodo")
                            .param("inicio", "2026-03-01T00:00:00")
                            .param("fim", "2026-03-31T23:59:59")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1));

            verify(alertaService).listarPorPeriodo(inicio, fim);
        }

        @Test
        @DisplayName("Deve retornar dashboard")
        void deveRetornarDashboard() throws Exception {
            Map<String, Object> dashboard = Map.of(
                    "totalAtivos", 5,
                    "altaGravidade", 2,
                    "mediaGravidade", 2,
                    "baixaGravidade", 1
            );

            when(alertaService.dashboard()).thenReturn(dashboard);

            mockMvc.perform(get("/api/v1/alertas/dashboard")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAtivos").value(5))
                    .andExpect(jsonPath("$.altaGravidade").value(2))
                    .andExpect(jsonPath("$.mediaGravidade").value(2))
                    .andExpect(jsonPath("$.baixaGravidade").value(1));

            verify(alertaService).dashboard();
        }
    }

    @Nested
    class AtualizacaoStatus {

        @Test
        @DisplayName("Deve marcar alerta como lido")
        void deveMarcarComoLido() throws Exception {
            Alerta alerta = criarAlerta(
                    1L,
                    TipoAlerta.EXCESSO_VELOCIDADE,
                    SeveridadeAlerta.ALTO,
                    "Excesso de velocidade"
            );
            alerta.setLido(true);
            alerta.setDataHoraLeitura(LocalDateTime.of(2026, 3, 30, 10, 30, 0));

            when(alertaService.marcarComoLido(1L)).thenReturn(alerta);

            mockMvc.perform(put("/api/v1/alertas/1/ler")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.lido").value(true));

            verify(alertaService).marcarComoLido(1L);
        }

        @Test
        @DisplayName("Deve resolver alerta")
        void deveResolverAlerta() throws Exception {
            Alerta alerta = criarAlerta(
                    2L,
                    TipoAlerta.GPS_SEM_SINAL,
                    SeveridadeAlerta.ALTO,
                    "GPS sem sinal"
            );
            alerta.setResolvido(true);
            alerta.setDataHoraResolucao(LocalDateTime.of(2026, 3, 30, 11, 0, 0));

            when(alertaService.resolverAlerta(2L)).thenReturn(alerta);

            mockMvc.perform(put("/api/v1/alertas/2/resolver")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.resolvido").value(true));

            verify(alertaService).resolverAlerta(2L);
        }
    }
}