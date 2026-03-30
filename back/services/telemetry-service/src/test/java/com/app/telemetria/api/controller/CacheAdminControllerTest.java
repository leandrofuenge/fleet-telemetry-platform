package com.app.telemetria.api.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.app.telemetria.application.service.CacheWarmingService;

@ExtendWith(MockitoExtension.class)
class CacheAdminControllerTest {

    @Mock
    private CacheWarmingService cacheWarmingService;

    @InjectMocks
    private CacheAdminController cacheAdminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(cacheAdminController)
                .build();
    }

    @Nested
    class WarmUpCache {

        @Test
        @DisplayName("Deve executar cache warming com sucesso")
        void deveExecutarCacheWarmingComSucesso() throws Exception {
            doNothing().when(cacheWarmingService).warmUpAllCaches();

            mockMvc.perform(post("/api/v1/admin/cache/warm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("success")))
                    .andExpect(jsonPath("$.message", is("Cache warming executado com sucesso")))
                    .andExpect(jsonPath("$.timeMs", notNullValue()))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            verify(cacheWarmingService).warmUpAllCaches();
        }

        @Test
        @DisplayName("Deve retornar erro interno quando cache warming falhar")
        void deveRetornarErroInternoQuandoCacheWarmingFalhar() throws Exception {
            doThrow(new RuntimeException("falha no aquecimento"))
                    .when(cacheWarmingService)
                    .warmUpAllCaches();

            mockMvc.perform(post("/api/v1/admin/cache/warm"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status", is("error")))
                    .andExpect(jsonPath("$.message", is("Erro ao executar cache warming: falha no aquecimento")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            verify(cacheWarmingService).warmUpAllCaches();
        }
    }
}