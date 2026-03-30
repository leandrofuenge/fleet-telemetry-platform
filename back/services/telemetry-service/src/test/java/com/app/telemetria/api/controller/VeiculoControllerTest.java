package com.app.telemetria.api.controller;

import com.app.telemetria.api.dto.response.VeiculoDTO;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.exception.VeiculoDuplicateException;
import com.app.telemetria.domain.exception.VeiculoNotFoundException;
import com.app.telemetria.domain.service.VeiculoService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeiculoControllerTest {

    @Mock
    private VeiculoService service;

    @InjectMocks
    private VeiculoController controller;

    private Veiculo veiculo;
    private VeiculoDTO veiculoDTO;

    @BeforeEach
    void setUp() {
        // ✅ Apenas campos essenciais (que existem no DTO)
        veiculo = new Veiculo();
        veiculo.setId(1L);
        veiculo.setPlaca("ABC-1234");
        veiculo.setModelo("Fiat Strada");
        veiculo.setCapacidadeCarga(1000.0);

        veiculoDTO = new VeiculoDTO();
        veiculoDTO.setId(1L);
        veiculoDTO.setPlaca("ABC-1234");
        veiculoDTO.setModelo("Fiat Strada");
        veiculoDTO.setCapacidadeCarga(1000.0);
    }

    @Test
    void criar_Sucesso() {
        when(service.salvar(any(Veiculo.class))).thenReturn(veiculoDTO);

        ResponseEntity<VeiculoDTO> response = controller.criar(veiculo);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(veiculoDTO, response.getBody());
        verify(service).salvar(any(Veiculo.class));
    }

    @Test
    void criar_PlacaVazia() {
        veiculo.setPlaca("");

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(veiculo));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Placa é obrigatória", exception.getMessage());
        verify(service, never()).salvar(any());
    }

    @Test
    void criar_ModeloVazio() {
        veiculo.setModelo("");

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(veiculo));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Modelo é obrigatório", exception.getMessage());
    }

    @Test
    void criar_CapacidadeCargaZero() {
        veiculo.setCapacidadeCarga(0.0);

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(veiculo));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("Capacidade de carga deve ser maior que zero", exception.getMessage());
    }

    @Test
    void criar_VeiculoDuplicado() {
        when(service.salvar(any(Veiculo.class)))
            .thenThrow(new VeiculoDuplicateException("Placa já cadastrada"));

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.criar(veiculo));
        
        assertEquals(ErrorCode.VEICULO_DUPLICATE, exception.getErrorCode());
        verify(service).salvar(any(Veiculo.class));
    }

    @Test
    void listar_Sucesso() {
        List<VeiculoDTO> veiculos = List.of(veiculoDTO);
        when(service.listarTodos()).thenReturn(veiculos);

        ResponseEntity<List<VeiculoDTO>> response = controller.listar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(service).listarTodos();
    }

    @Test
    void listar_NenhumVeiculo() {
        when(service.listarTodos()).thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.listar());
        
        assertEquals(ErrorCode.VEICULO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void buscar_Sucesso() {
        Long id = 1L;
        when(service.buscarPorId(id)).thenReturn(veiculoDTO);

        ResponseEntity<VeiculoDTO> response = controller.buscar(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(veiculoDTO, response.getBody());
        verify(service).buscarPorId(id);
    }

    @Test
    void buscar_NaoEncontrado() {
        Long id = 999L;
        when(service.buscarPorId(id))
            .thenThrow(new VeiculoNotFoundException("Veículo não encontrado"));

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.buscar(id));
        
        assertEquals(ErrorCode.VEICULO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void buscarPorPlaca_Sucesso() {
        String placa = "ABC-1234";
        when(service.buscarPorPlaca(placa)).thenReturn(veiculoDTO);

        ResponseEntity<VeiculoDTO> response = controller.buscarPorPlaca(placa);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(veiculoDTO, response.getBody());
        verify(service).buscarPorPlaca(placa);
    }

    @Test
    void buscarPorPlaca_NaoEncontrado() {
        String placa = "XYZ-9999";
        when(service.buscarPorPlaca(placa))
            .thenThrow(new VeiculoNotFoundException("Veículo não encontrado"));

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.buscarPorPlaca(placa));
        
        assertEquals(ErrorCode.VEICULO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void atualizar_Sucesso() {
        Long id = 1L;
        Veiculo veiculoAtualizado = new Veiculo();
        veiculoAtualizado.setPlaca("ABC-1235");
        veiculoAtualizado.setModelo("Fiat Toro");
        veiculoAtualizado.setCapacidadeCarga(1500.0);
        
        VeiculoDTO dtoAtualizado = new VeiculoDTO();
        dtoAtualizado.setId(id);
        dtoAtualizado.setPlaca("ABC-1235");
        dtoAtualizado.setModelo("Fiat Toro");
        dtoAtualizado.setCapacidadeCarga(1500.0);
        
        when(service.buscarPorId(id)).thenReturn(veiculoDTO);
        when(service.atualizar(eq(id), any(Veiculo.class))).thenReturn(dtoAtualizado);

        ResponseEntity<VeiculoDTO> response = controller.atualizar(id, veiculoAtualizado);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ABC-1235", response.getBody().getPlaca());
        verify(service).buscarPorId(id);
        verify(service).atualizar(eq(id), any(Veiculo.class));
    }

    @Test
    void atualizar_PlacaVazia() {
        Long id = 1L;
        Veiculo veiculoAtualizado = new Veiculo();
        veiculoAtualizado.setPlaca("");

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, veiculoAtualizado));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(service, never()).buscarPorId(anyLong());
    }

    @Test
    void atualizar_CapacidadeCargaNegativa() {
        Long id = 1L;
        Veiculo veiculoAtualizado = new Veiculo();
        veiculoAtualizado.setCapacidadeCarga(-100.0);

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, veiculoAtualizado));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(service, never()).buscarPorId(anyLong());
    }

    @Test
    void atualizar_NaoEncontrado() {
        Long id = 999L;
        when(service.buscarPorId(id))
            .thenThrow(new VeiculoNotFoundException("Veículo não encontrado"));

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.atualizar(id, veiculo));
        
        assertEquals(ErrorCode.VEICULO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deletar_Sucesso() {
        Long id = 1L;
        doNothing().when(service).deletar(eq(id));

        ResponseEntity<Void> response = controller.deletar(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).deletar(eq(id));
    }

    @Test
    void deletar_NaoEncontrado() {
        Long id = 999L;
        doThrow(new VeiculoNotFoundException("Veículo não encontrado"))
            .when(service).deletar(eq(id));

        BusinessException exception = assertThrows(BusinessException.class, 
            () -> controller.deletar(id));
        
        assertEquals(ErrorCode.VEICULO_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void status_Sucesso() {
        ResponseEntity<String> response = controller.status();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Veículo service está operacional", response.getBody());
    }

    @Test
    void contarVeiculos_Sucesso() {
        List<VeiculoDTO> veiculos = List.of(veiculoDTO, criarVeiculoDTO(2L));
        when(service.listarTodos()).thenReturn(veiculos);

        ResponseEntity<Long> response = controller.contarVeiculos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2L, response.getBody());
        verify(service).listarTodos();
    }

    private VeiculoDTO criarVeiculoDTO(Long id) {
        VeiculoDTO dto = new VeiculoDTO();
        dto.setId(id);
        dto.setPlaca("DEF-5678");
        dto.setModelo("VW Delivery");
        return dto;
    }
}