package com.app.telemetria.api.controller;

import com.app.telemetria.domain.entity.Telemetria;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.app.telemetria.infrastructure.persistence.VeiculoRepository;
import com.app.telemetria.infrastructure.persistence.ViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetriaControllerTest {

    @Mock
    private TelemetriaRepository telemetriaRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ViagemRepository viagemRepository;

    @Mock
    private KafkaTemplate<String, TelemetriaController.TelemetriaRequest> kafkaTemplate;

    @InjectMocks
    private TelemetriaController controller;

    private Veiculo veiculo;
    private TelemetriaController.TelemetriaRequest request;
    private TelemetriaController.VeiculoRequest veiculoRequest;

    @BeforeEach
    void setUp() {
        veiculo = new Veiculo();
        veiculo.setId(1L);

        veiculoRequest = new TelemetriaController.VeiculoRequest();
        veiculoRequest.setId(1L);

        request = new TelemetriaController.TelemetriaRequest();
        request.setVeiculo(veiculoRequest);
        request.setLatitude(-23.5505);
        request.setLongitude(-46.6333);
        request.setVelocidade(60.0);
        request.setDataHora(LocalDateTime.now());
    }

    @Test
    void criar_Sucesso() {
        // Arrange
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));

        SendResult<String, TelemetriaController.TelemetriaRequest> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, TelemetriaController.TelemetriaRequest>> future = 
            CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq("telemetria-raw"), eq("1"), any(TelemetriaController.TelemetriaRequest.class)))
                .thenReturn(future);

        try (MockedStatic<CompletableFuture> mockedCompletableFuture = mockStatic(CompletableFuture.class)) {
            // ✅ Mock runAsync para executar SINCRO (sem thread real)
            mockedCompletableFuture.when(() -> CompletableFuture.runAsync(any(Runnable.class)))
                                   .thenAnswer(invocation -> {
                                       Runnable runnable = invocation.getArgument(0, Runnable.class);
                                       runnable.run(); // Executa imediatamente
                                       return null;
                                   });

            // Act
            ResponseEntity<String> response = controller.criar(request);

            // Assert
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
            assertTrue(response.getBody().contains("ID do veículo: 1"));
            verify(veiculoRepository).findById(1L);
            verify(kafkaTemplate).send(eq("telemetria-raw"), eq("1"), any(TelemetriaController.TelemetriaRequest.class));
        }
    }

    @Test
    void criar_VeiculoIdNulo() {
        veiculoRequest.setId(null);
        
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(request));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(veiculoRepository, never()).findById(anyLong());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void criar_VeiculoNaoEncontrado() {
        when(veiculoRepository.findById(1L)).thenReturn(Optional.empty());
        
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(request));
        
        assertEquals(ErrorCode.VEICULO_NOT_FOUND, exception.getErrorCode());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void criar_LatitudeNula() {
        request.setLatitude(null);
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(request));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Latitude e longitude são obrigatórios", exception.getMessage());
    }

    @Test
    void criar_LatitudeInvalida() {
        request.setLatitude(100.0);
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(request));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Latitude deve estar entre -90 e 90", exception.getMessage());
    }

    @Test
    void listarPorVeiculo_Sucesso() {
        Long veiculoId = 1L;
        List<Telemetria> telemetrias = List.of(criarTelemetria(1L));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(telemetriaRepository.findByVeiculoIdOrderByDataHoraDesc(veiculoId)).thenReturn(telemetrias);

        List<Telemetria> resultado = controller.listarPorVeiculo(veiculoId);

        assertEquals(1, resultado.size());
        verify(veiculoRepository).findById(veiculoId);
        verify(telemetriaRepository).findByVeiculoIdOrderByDataHoraDesc(veiculoId);
    }

    @Test
    void listarPorVeiculo_VeiculoNaoEncontrado() {
        Long veiculoId = 999L;
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listarPorVeiculo(veiculoId));
        
        assertEquals(ErrorCode.VEICULO_NOT_FOUND, exception.getErrorCode());
        verify(telemetriaRepository, never()).findByVeiculoIdOrderByDataHoraDesc(anyLong());
    }

    @Test
    void ultimaTelemetria_Sucesso() {
        Long veiculoId = 1L;
        Telemetria telemetria = criarTelemetria(1L);
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(veiculoId)).thenReturn(Optional.of(telemetria));

        ResponseEntity<Telemetria> response = controller.ultimaTelemetria(veiculoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(telemetria, response.getBody());
    }

    @Test
    void ultimaTelemetria_NaoEncontrado() {
        Long veiculoId = 1L;
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(veiculoId)).thenReturn(Optional.empty());

        ResponseEntity<Telemetria> response = controller.ultimaTelemetria(veiculoId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void listarPorPeriodo_Sucesso() {
        Long veiculoId = 1L;
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fim = LocalDateTime.now();
        List<Telemetria> telemetrias = List.of(criarTelemetria(1L));
        
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(telemetriaRepository.findByVeiculoIdAndDataHoraBetween(veiculoId, inicio, fim))
                .thenReturn(telemetrias);

        List<Telemetria> resultado = controller.listarPorPeriodo(veiculoId, inicio, fim);

        assertEquals(1, resultado.size());
        verify(telemetriaRepository).findByVeiculoIdAndDataHoraBetween(veiculoId, inicio, fim);
    }

    @Test
    void listarPorPeriodo_DataInicioMaiorQueFim() {
        Long veiculoId = 1L;
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = LocalDateTime.now().minusDays(1);

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listarPorPeriodo(veiculoId, inicio, fim));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(veiculoRepository, never()).findById(anyLong());
    }

    @Test
    void status_Sucesso() {
        ResponseEntity<String> response = controller.status();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Telemetria service está operacional", response.getBody());
    }

    @Test
    void testarKafka_Sucesso() {
        // Arrange - Mock simples para testarKafka (sem CompletableFuture complexo)
        SendResult<String, TelemetriaController.TelemetriaRequest> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, TelemetriaController.TelemetriaRequest>> future = 
            CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("telemetria-raw"), eq("test"), any()))
                .thenReturn(future);

        // Act
        ResponseEntity<String> response = controller.testarKafka();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Mensagem de teste enviada"));
        verify(kafkaTemplate).send(eq("telemetria-raw"), eq("test"), any());
    }

    private Telemetria criarTelemetria(Long veiculoId) {
        Telemetria telemetria = new Telemetria();
        telemetria.setId(1L);
        telemetria.setVeiculoId(veiculoId);
        telemetria.setLatitude(-23.5505);
        telemetria.setLongitude(-46.6333);
        telemetria.setVelocidade(60.0);
        telemetria.setDataHora(LocalDateTime.now());
        return telemetria;
    }
}