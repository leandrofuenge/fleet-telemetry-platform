package com.app.telemetria.api.controller;

import com.app.telemetria.api.dto.response.MotoristaDTO;
import com.app.telemetria.domain.entity.Motorista;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.exception.MotoristaDuplicateException;
import com.app.telemetria.domain.exception.MotoristaNotFoundException;
import com.app.telemetria.domain.service.MotoristaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotoristaControllerTest {

    @Mock
    private MotoristaService service;

    @InjectMocks
    private MotoristaController controller;

    private Motorista motorista;
    private MotoristaDTO dto;

    @BeforeEach
    void setUp() {
        motorista = new Motorista();
        motorista.setId(1L);
        motorista.setNome("João Silva");
        motorista.setCpf("123.456.789-00");
        motorista.setCnh("12345678900");
        motorista.setCategoriaCnh("B");

        dto = new MotoristaDTO();
        dto.setNome("João Silva");
        dto.setCpf("123.456.789-00");
        dto.setCnh("12345678900");
        dto.setCategoriaCnh("B");
    }

    @Test
    void criar_Sucesso() {
        // Arrange
        when(service.salvar(any(Motorista.class))).thenReturn(motorista);

        // Act
        ResponseEntity<Motorista> response = controller.criar(dto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(motorista, response.getBody());
        verify(service).salvar(any(Motorista.class));
    }

    @Test
    void criar_NomeVazio() {
        // Arrange
        dto.setNome("");
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(dto));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Nome é obrigatório", exception.getMessage());
        verify(service, never()).salvar(any());
    }

    @Test
    void criar_CpfVazio() {
        // Arrange
        dto.setCpf("");
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(dto));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("CPF é obrigatório", exception.getMessage());
    }

    @Test
    void criar_CnhVazia() {
        // Arrange
        dto.setCnh("");
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(dto));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("CNH é obrigatória", exception.getMessage());
    }

    @Test
    void criar_CategoriaCnhVazia() {
        // Arrange
        dto.setCategoriaCnh("");
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(dto));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Categoria da CNH é obrigatória", exception.getMessage());
    }

    @Test
    void criar_MotoristaDuplicado() {
        // Arrange
        when(service.salvar(any(Motorista.class)))
            .thenThrow(new MotoristaDuplicateException("CPF já cadastrado"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(dto));
        
        assertEquals(ErrorCode.MOTORISTA_DUPLICATE, exception.getErrorCode());
        verify(service).salvar(any(Motorista.class));
    }

    @Test
    void listar_Sucesso() {
        // Arrange
        List<Motorista> motoristas = List.of(motorista);
        when(service.listar()).thenReturn(motoristas);

        // Act
        List<Motorista> resultado = controller.listar();

        // Assert
        assertEquals(1, resultado.size());
        assertEquals(motorista, resultado.get(0));
        verify(service).listar();
    }

    @Test
    void listar_NenhumMotorista() {
        // Arrange
        when(service.listar()).thenReturn(Collections.emptyList());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listar());
        
        assertEquals(ErrorCode.MOTORISTA_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void buscar_Sucesso() {
        // Arrange
        Long id = 1L;
        when(service.buscarPorId(id)).thenReturn(motorista);

        // Act
        Motorista resultado = controller.buscar(id);

        // Assert
        assertEquals(motorista, resultado);
        verify(service).buscarPorId(id);
    }

    @Test
    void buscar_NaoEncontrado() {
        // Arrange
        Long id = 999L;
        when(service.buscarPorId(id))
            .thenThrow(new MotoristaNotFoundException("Motorista não encontrado"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.buscar(id));
        
        assertEquals(ErrorCode.MOTORISTA_NOT_FOUND, exception.getErrorCode());
        verify(service).buscarPorId(id);
    }

    @Test
    void buscarPorCpf_Sucesso() {
        // Arrange
        String cpf = "123.456.789-00";
        when(service.buscarPorCpf(cpf)).thenReturn(motorista);

        // Act
        Motorista resultado = controller.buscarPorCpf(cpf);

        // Assert
        assertEquals(motorista, resultado);
        verify(service).buscarPorCpf(cpf);
    }

    @Test
    void buscarPorCpf_NaoEncontrado() {
        // Arrange
        String cpf = "999.999.999-99";
        when(service.buscarPorCpf(cpf))
            .thenThrow(new MotoristaNotFoundException("Motorista não encontrado"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.buscarPorCpf(cpf));
        
        assertEquals(ErrorCode.MOTORISTA_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void atualizar_Sucesso() {
        // Arrange
        Long id = 1L;
        dto.setNome("João Silva Atualizado");
        when(service.buscarPorId(id)).thenReturn(motorista);
        when(service.salvar(any(Motorista.class))).thenReturn(motorista);

        // Act
        ResponseEntity<Motorista> response = controller.atualizar(id, dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("João Silva Atualizado", response.getBody().getNome());
        verify(service).buscarPorId(id);
        verify(service).salvar(any(Motorista.class));
    }

    @Test
    void atualizar_NomeVazio() {
        // Arrange
        Long id = 1L;
        dto.setNome("");
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, dto));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(service, never()).buscarPorId(anyLong());
    }

    @Test
    void atualizar_NaoEncontrado() {
        // Arrange
        Long id = 999L;
        when(service.buscarPorId(id))
            .thenThrow(new MotoristaNotFoundException("Motorista não encontrado"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, dto));
        
        assertEquals(ErrorCode.MOTORISTA_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void atualizar_Duplicado() {
        // Arrange
        Long id = 1L;
        when(service.buscarPorId(id)).thenReturn(motorista);
        when(service.salvar(any(Motorista.class)))
            .thenThrow(new MotoristaDuplicateException("CPF já existe"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, dto));
        
        assertEquals(ErrorCode.MOTORISTA_DUPLICATE, exception.getErrorCode());
    }

    @Test
    void deletar_Sucesso() {
        // Arrange
        Long id = 1L;
        doNothing().when(service).deletar(id);

        // Act
        ResponseEntity<Void> response = controller.deletar(id);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).deletar(id);
    }

    @Test
    void deletar_NaoEncontrado() {
        // Arrange
        Long id = 999L;
        doThrow(new MotoristaNotFoundException("Motorista não encontrado"))
            .when(service).deletar(id);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.deletar(id));
        
        assertEquals(ErrorCode.MOTORISTA_NOT_FOUND, exception.getErrorCode());
    }
}