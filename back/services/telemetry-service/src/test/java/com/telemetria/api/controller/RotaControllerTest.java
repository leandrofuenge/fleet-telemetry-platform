package com.telemetria.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.exception.RotaDuplicateException;
import com.telemetria.domain.exception.RotaNotFoundException;
import com.telemetria.domain.exception.RotaValidationException;
import com.telemetria.domain.service.RotaService;

@ExtendWith(MockitoExtension.class)
class RotaControllerTest {

    @Mock
    private RotaService service;

    @InjectMocks
    private RotaController controller;

    private Rota rota;

    @BeforeEach
    void setUp() {
        rota = new Rota();
        rota.setId(1L);
        rota.setNome("Rota SP-RJ");
        rota.setOrigem("São Paulo");
        rota.setDestino("Rio de Janeiro");
    }

    @Test
    void criar_Sucesso() {
        // Arrange
        when(service.salvar(any(Rota.class))).thenReturn(rota);

        // Act
        ResponseEntity<Rota> response = controller.criar(rota);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(rota, response.getBody());
        verify(service).salvar(any(Rota.class));
    }

    @Test
    void criar_NomeVazio() {
        // Arrange
        rota.setNome("");
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(rota));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Nome da rota é obrigatório", exception.getMessage());
        verify(service, never()).salvar(any());
    }

    @Test
    void criar_OrigemNula() {
        // Arrange
        rota.setOrigem(null);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(rota));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Origem e destino são obrigatórios", exception.getMessage());
    }

    @Test
    void criar_DestinoNulo() {
        // Arrange
        rota.setDestino(null);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(rota));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Origem e destino são obrigatórios", exception.getMessage());
    }

    @Test
    void criar_RotaDuplicada() {
        // Arrange
        when(service.salvar(any(Rota.class)))
            .thenThrow(new RotaDuplicateException("Rota já existe"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(rota));
        
        assertEquals(ErrorCode.ROTA_DUPLICATE, exception.getErrorCode());
        verify(service).salvar(any(Rota.class));
    }

    @Test
    void criar_RotaInvalida() {
        // Arrange
        when(service.salvar(any(Rota.class)))
            .thenThrow(new RotaValidationException("Distância inválida"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(rota));
        
        assertEquals(ErrorCode.ROTA_INVALID, exception.getErrorCode());
    }

    @Test
    void listar_Sucesso() {
        // Arrange
        List<Rota> rotas = List.of(rota);
        when(service.listar()).thenReturn(rotas);

        // Act
        ResponseEntity<List<Rota>> response = controller.listar();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(service).listar();
    }

    @Test
    void listar_NenhumaRota() {
        // Arrange
        when(service.listar()).thenReturn(Collections.emptyList());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listar());
        
        assertEquals(ErrorCode.ROTA_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void buscar_Sucesso() {
        // Arrange
        Long id = 1L;
        when(service.buscarPorId(id)).thenReturn(rota);

        // Act
        ResponseEntity<Rota> response = controller.buscar(id);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(rota, response.getBody());
        verify(service).buscarPorId(id);
    }

    @Test
    void buscar_NaoEncontrado() {
        // Arrange
        Long id = 999L;
        when(service.buscarPorId(id))
            .thenThrow(new RotaNotFoundException("Rota não encontrada"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.buscar(id));
        
        assertEquals(ErrorCode.ROTA_NOT_FOUND, exception.getErrorCode());
        verify(service).buscarPorId(id);
    }

    @Test
    void atualizar_Sucesso() {
        // Arrange
        Long id = 1L;
        Rota rotaAtualizada = new Rota();
        rotaAtualizada.setId(id);
        rotaAtualizada.setNome("Rota SP-RJ Atualizada");
        rotaAtualizada.setOrigem("São Paulo - Centro");
        rotaAtualizada.setDestino("Rio de Janeiro");
        
        when(service.atualizar(eq(id), any(Rota.class))).thenReturn(rotaAtualizada);

        // Act
        ResponseEntity<Rota> response = controller.atualizar(id, rotaAtualizada);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Rota SP-RJ Atualizada", response.getBody().getNome());
        verify(service).atualizar(eq(id), any(Rota.class));
    }

    @Test
    void atualizar_NomeVazio() {
        // Arrange
        Long id = 1L;
        Rota rotaAtualizada = new Rota();
        rotaAtualizada.setNome(""); // Nome vazio
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, rotaAtualizada));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Nome da rota não pode ser vazio", exception.getMessage());
        verify(service, never()).atualizar(anyLong(), any());
    }

    @Test
    void atualizar_NaoEncontrado() {
        // Arrange
        Long id = 999L;
        when(service.atualizar(eq(id), any(Rota.class)))
            .thenThrow(new RotaNotFoundException("Rota não encontrada"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, rota));
        
        assertEquals(ErrorCode.ROTA_NOT_FOUND, exception.getErrorCode());
        verify(service).atualizar(eq(id), any(Rota.class));
    }

    @Test
    void atualizar_Duplicado() {
        // Arrange
        Long id = 1L;
        when(service.atualizar(eq(id), any(Rota.class)))
            .thenThrow(new RotaDuplicateException("Nome da rota já existe"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, rota));
        
        assertEquals(ErrorCode.ROTA_DUPLICATE, exception.getErrorCode());
    }

    @Test
    void atualizar_Invalido() {
        // Arrange
        Long id = 1L;
        when(service.atualizar(eq(id), any(Rota.class)))
            .thenThrow(new RotaValidationException("Distância inválida"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, rota));
        
        assertEquals(ErrorCode.ROTA_INVALID, exception.getErrorCode());
    }

    @Test
    void deletar_Sucesso() {
        // Arrange
        Long id = 1L;
        doNothing().when(service).deletar(eq(id));

        // Act
        ResponseEntity<Void> response = controller.deletar(id);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).deletar(eq(id));
    }

    @Test
    void deletar_NaoEncontrado() {
        // Arrange
        Long id = 999L;
        doThrow(new RotaNotFoundException("Rota não encontrada"))
            .when(service).deletar(eq(id));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.deletar(id));
        
        assertEquals(ErrorCode.ROTA_NOT_FOUND, exception.getErrorCode());
        verify(service).deletar(eq(id));
    }
}