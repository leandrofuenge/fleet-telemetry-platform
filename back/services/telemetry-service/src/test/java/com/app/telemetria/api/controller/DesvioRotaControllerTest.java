package com.app.telemetria.api.controller;

import com.app.telemetria.domain.entity.DesvioRota;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.service.RotaService;
import com.app.telemetria.domain.service.VeiculoService;
import com.app.telemetria.infrastructure.persistence.DesvioRotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DesvioRotaControllerTest {

    @Mock
    private DesvioRotaRepository desvioRotaRepository;

    @Mock
    private RotaService rotaService;

    @Mock
    private VeiculoService veiculoService;

    @InjectMocks
    private DesvioRotaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listarDesviosPorRota_Sucesso() {
        // Arrange
        Long rotaId = 1L;
        List<DesvioRota> desvios = List.of(criarDesvioRota(1L, rotaId, 10L));
        
        when(rotaService.buscarPorId(rotaId)).thenReturn(null); // Não lança exceção
        when(desvioRotaRepository.findByRotaIdOrderByDataHoraDesvioDesc(rotaId)).thenReturn(desvios);

        // Act
        List<DesvioRota> resultado = controller.listarDesviosPorRota(rotaId);

        // Assert
        assertEquals(1, resultado.size());
        assertEquals(rotaId, resultado.get(0).getRotaId());
        verify(rotaService).buscarPorId(rotaId);
        verify(desvioRotaRepository).findByRotaIdOrderByDataHoraDesvioDesc(rotaId);
    }

    @Test
    void listarDesviosPorRota_RotaNaoEncontrada() {
        // Arrange
        Long rotaId = 999L;
        when(rotaService.buscarPorId(rotaId)).thenThrow(new RuntimeException("Rota não encontrada"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listarDesviosPorRota(rotaId));
        
        assertEquals(ErrorCode.ROTA_NOT_FOUND, exception.getErrorCode());
        verify(desvioRotaRepository, never()).findByRotaIdOrderByDataHoraDesvioDesc(anyLong());
    }

    @Test
    void listarDesviosPorRota_NenhumDesvio() {
        // Arrange
        Long rotaId = 1L;
        when(rotaService.buscarPorId(rotaId)).thenReturn(null);
        when(desvioRotaRepository.findByRotaIdOrderByDataHoraDesvioDesc(rotaId)).thenReturn(Collections.emptyList());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listarDesviosPorRota(rotaId));
        
        assertEquals(ErrorCode.DESVIO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void listarDesviosAtivos_Sucesso() {
        // Arrange
        List<DesvioRota> desviosAtivos = List.of(criarDesvioRota(1L, 1L, 10L));
        when(desvioRotaRepository.findByResolvidoFalse()).thenReturn(desviosAtivos);

        // Act
        List<DesvioRota> resultado = controller.listarDesviosAtivos();

        // Assert
        assertEquals(1, resultado.size());
        verify(desvioRotaRepository).findByResolvidoFalse();
    }

    @Test
    void listarDesviosAtivos_NenhumDesvio() {
        // Arrange
        when(desvioRotaRepository.findByResolvidoFalse()).thenReturn(Collections.emptyList());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listarDesviosAtivos());
        
        assertEquals(ErrorCode.DESVIO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void listarDesviosPorVeiculo_Sucesso() {
        // Arrange
        Long veiculoId = 10L;
        Long rotaId = 1L;
        List<DesvioRota> desvios = List.of(criarDesvioRota(1L, rotaId, veiculoId));
        
        when(veiculoService.buscarPorId(veiculoId)).thenReturn(null);
        when(desvioRotaRepository.findByVeiculoIdOrderByDataHoraDesvioDesc(veiculoId)).thenReturn(desvios);

        // Act
        List<DesvioRota> resultado = controller.listarDesviosPorVeiculo(veiculoId);

        // Assert
        assertEquals(1, resultado.size());
        assertEquals(veiculoId, resultado.get(0).getVeiculoId());
        verify(veiculoService).buscarPorId(veiculoId);
        verify(desvioRotaRepository).findByVeiculoIdOrderByDataHoraDesvioDesc(veiculoId);
    }

    @Test
    void listarDesviosPorVeiculo_VeiculoNaoEncontrado() {
        // Arrange
        Long veiculoId = 999L;
        when(veiculoService.buscarPorId(veiculoId)).thenThrow(new RuntimeException("Veículo não encontrado"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listarDesviosPorVeiculo(veiculoId));
        
        assertEquals(ErrorCode.VEICULO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void buscarDesvioPorId_Sucesso() {
        // Arrange
        Long id = 1L;
        Long rotaId = 1L;
        Long veiculoId = 10L;
        DesvioRota desvio = criarDesvioRota(id, rotaId, veiculoId);
        when(desvioRotaRepository.findById(id)).thenReturn(Optional.of(desvio));

        // Act
        DesvioRota resultado = controller.buscarDesvioPorId(id);

        // Assert
        assertEquals(id, resultado.getId());
        verify(desvioRotaRepository).findById(id);
    }

    @Test
    void buscarDesvioPorId_NaoEncontrado() {
        // Arrange
        Long id = 999L;
        when(desvioRotaRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.buscarDesvioPorId(id));
        
        assertEquals(ErrorCode.DESVIO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void resolverDesvio_Sucesso() {
        // Arrange
        Long id = 1L;
        Long rotaId = 1L;
        Long veiculoId = 10L;
        DesvioRota desvio = criarDesvioRota(id, rotaId, veiculoId);
        desvio.setResolvido(false);
        
        when(desvioRotaRepository.findById(id)).thenReturn(Optional.of(desvio));
        when(desvioRotaRepository.save(any(DesvioRota.class))).thenReturn(desvio);

        // Act
        DesvioRota resultado = controller.resolverDesvio(id);

        // Assert
        assertTrue(resultado.isResolvido());
        assertNotNull(resultado.getDataHoraRetorno());
        verify(desvioRotaRepository).save(any(DesvioRota.class));
    }

    @Test
    void resolverDesvio_NaoEncontrado() {
        // Arrange
        Long id = 999L;
        when(desvioRotaRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.resolverDesvio(id));
        
        assertEquals(ErrorCode.DESVIO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void resolverDesvio_JaResolvido() {
        // Arrange
        Long id = 1L;
        DesvioRota desvio = criarDesvioRota(id, 1L, 10L);
        desvio.setResolvido(true);
        
        when(desvioRotaRepository.findById(id)).thenReturn(Optional.of(desvio));
        when(desvioRotaRepository.save(any(DesvioRota.class))).thenReturn(desvio);

        // Act
        DesvioRota resultado = controller.resolverDesvio(id);

        // Assert
        assertTrue(resultado.isResolvido());
        // DataHoraRetorno deve ser atualizada mesmo que já estava resolvido
        verify(desvioRotaRepository).save(any(DesvioRota.class));
    }

    private DesvioRota criarDesvioRota(Long id, Long rotaId, Long veiculoId) {
        DesvioRota desvio = new DesvioRota();
        desvio.setId(id);
        desvio.setRotaId(rotaId);
        desvio.setVeiculoId(veiculoId);
        desvio.setDataHoraDesvio(LocalDateTime.now().minusHours(1));
        desvio.setResolvido(false);
        return desvio;
    }
}