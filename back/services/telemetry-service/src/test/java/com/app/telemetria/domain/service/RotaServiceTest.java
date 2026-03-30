package com.app.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

import com.app.telemetria.domain.entity.Rota;
import com.app.telemetria.domain.exception.RotaDuplicateException;
import com.app.telemetria.domain.exception.RotaNotFoundException;
import com.app.telemetria.domain.exception.RotaValidationException;
import com.app.telemetria.infrastructure.persistence.RotaRepository;

@ExtendWith(MockitoExtension.class)
class RotaServiceTest {

    @Mock
    private RotaRepository rotaRepository;

    @InjectMocks
    private RotaService rotaService;

    private Rota rota;

    @BeforeEach
    void setup() {
        rota = new Rota();
        rota.setId(1L);
        rota.setNome("Rota Centro");
        rota.setOrigem("Cuiabá");
        rota.setDestino("Várzea Grande");
        rota.setLatitudeOrigem(-15.601);
        rota.setLongitudeOrigem(-56.097);
        rota.setLatitudeDestino(-15.646);
        rota.setLongitudeDestino(-56.132);
        rota.setDistanciaPrevista(12.5);
        rota.setTempoPrevisto(25);
        rota.setStatus("ATIVA");
        rota.setAtiva(true);
    }

    @Nested
    class Consultas {

        @Test
        @DisplayName("Deve listar todas as rotas")
        void deveListarRotas() {
            when(rotaRepository.findAll()).thenReturn(List.of(rota));

            List<Rota> resultado = rotaService.listar();

            assertEquals(1, resultado.size());
            assertEquals("Rota Centro", resultado.get(0).getNome());
            verify(rotaRepository).findAll();
        }

        @Test
        @DisplayName("Deve buscar rota por ID")
        void deveBuscarPorId() {
            when(rotaRepository.findById(1L)).thenReturn(Optional.of(rota));

            Rota resultado = rotaService.buscarPorId(1L);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            assertEquals("Rota Centro", resultado.getNome());
            verify(rotaRepository).findById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando rota não for encontrada por ID")
        void deveLancarExcecaoAoBuscarPorIdInexistente() {
            when(rotaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RotaNotFoundException.class, () -> rotaService.buscarPorId(99L));

            verify(rotaRepository).findById(99L);
        }

        @Test
        @DisplayName("Deve buscar rotas por status")
        void deveBuscarPorStatus() {
            when(rotaRepository.findByStatus("EM_ANDAMENTO")).thenReturn(List.of(rota));

            List<Rota> resultado = rotaService.buscarPorStatus("EM_ANDAMENTO");

            assertEquals(1, resultado.size());
            verify(rotaRepository).findByStatus("EM_ANDAMENTO");
        }

        @Test
        @DisplayName("Deve buscar rotas ativas")
        void deveBuscarAtivas() {
            when(rotaRepository.findByAtivaTrue()).thenReturn(List.of(rota));

            List<Rota> resultado = rotaService.buscarAtivas();

            assertEquals(1, resultado.size());
            verify(rotaRepository).findByAtivaTrue();
        }

        @Test
        @DisplayName("Deve buscar rotas por veículo")
        void deveBuscarPorVeiculo() {
            when(rotaRepository.findByVeiculoId(10L)).thenReturn(List.of(rota));

            List<Rota> resultado = rotaService.buscarPorVeiculo(10L);

            assertEquals(1, resultado.size());
            verify(rotaRepository).findByVeiculoId(10L);
        }

        @Test
        @DisplayName("Deve buscar rotas por motorista")
        void deveBuscarPorMotorista() {
            when(rotaRepository.findByMotoristaId(20L)).thenReturn(List.of(rota));

            List<Rota> resultado = rotaService.buscarPorMotorista(20L);

            assertEquals(1, resultado.size());
            verify(rotaRepository).findByMotoristaId(20L);
        }
    }

    @Nested
    class Salvamento {

        @Test
        @DisplayName("Deve salvar rota com sucesso")
        void deveSalvarRota() {
            Rota nova = new Rota();
            nova.setNome("Nova Rota");
            nova.setOrigem("Origem A");
            nova.setDestino("Destino B");

            Rota salva = new Rota();
            salva.setId(2L);
            salva.setNome("Nova Rota");
            salva.setOrigem("Origem A");
            salva.setDestino("Destino B");

            when(rotaRepository.countByNome("Nova Rota")).thenReturn(0L);
            when(rotaRepository.save(nova)).thenReturn(salva);

            Rota resultado = rotaService.salvar(nova);

            assertNotNull(resultado);
            assertEquals(2L, resultado.getId());
            verify(rotaRepository).countByNome("Nova Rota");
            verify(rotaRepository).save(nova);
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome for nulo")
        void deveLancarExcecaoQuandoNomeForNulo() {
            rota.setNome(null);

            assertThrows(RotaValidationException.class, () -> rotaService.salvar(rota));

            verify(rotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome for vazio")
        void deveLancarExcecaoQuandoNomeForVazio() {
            rota.setNome("   ");

            assertThrows(RotaValidationException.class, () -> rotaService.salvar(rota));

            verify(rotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando origem for nula")
        void deveLancarExcecaoQuandoOrigemForNula() {
            rota.setOrigem(null);

            assertThrows(RotaValidationException.class, () -> rotaService.salvar(rota));

            verify(rotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando destino for nulo")
        void deveLancarExcecaoQuandoDestinoForNulo() {
            rota.setDestino(null);

            assertThrows(RotaValidationException.class, () -> rotaService.salvar(rota));

            verify(rotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome da rota já existir")
        void deveLancarExcecaoQuandoNomeJaExistir() {
            when(rotaRepository.countByNome("Rota Centro")).thenReturn(1L);

            assertThrows(RotaDuplicateException.class, () -> rotaService.salvar(rota));

            verify(rotaRepository).countByNome("Rota Centro");
            verify(rotaRepository, never()).save(any());
        }
    }

    @Nested
    class Atualizacao {

        @Test
        @DisplayName("Deve atualizar rota com sucesso")
        void deveAtualizarRota() {
            Rota dados = new Rota();
            dados.setNome("Rota Atualizada");
            dados.setOrigem("Nova Origem");
            dados.setDestino("Novo Destino");
            dados.setLatitudeOrigem(-15.500);
            dados.setLongitudeOrigem(-56.000);
            dados.setLatitudeDestino(-15.700);
            dados.setLongitudeDestino(-56.200);
            dados.setDistanciaPrevista(30.0);
            dados.setTempoPrevisto(40);
            dados.setStatus("EM_ANDAMENTO");
            dados.setAtiva(false);

            when(rotaRepository.findById(1L)).thenReturn(Optional.of(rota));
            when(rotaRepository.countByNomeAndIdNot("Rota Atualizada", 1L)).thenReturn(0L);
            when(rotaRepository.save(any(Rota.class))).thenAnswer(inv -> inv.getArgument(0));

            Rota resultado = rotaService.atualizar(1L, dados);

            assertEquals("Rota Atualizada", resultado.getNome());
            assertEquals("Nova Origem", resultado.getOrigem());
            assertEquals("Novo Destino", resultado.getDestino());
            assertEquals(-15.500, resultado.getLatitudeOrigem());
            assertEquals(-56.000, resultado.getLongitudeOrigem());
            assertEquals(-15.700, resultado.getLatitudeDestino());
            assertEquals(-56.200, resultado.getLongitudeDestino());
            assertEquals(30.0, resultado.getDistanciaPrevista());
            assertEquals(40, resultado.getTempoPrevisto());
            assertEquals("EM_ANDAMENTO", resultado.getStatus());
            assertFalse(resultado.getAtiva());

            verify(rotaRepository).findById(1L);
            verify(rotaRepository).countByNomeAndIdNot("Rota Atualizada", 1L);
            verify(rotaRepository).save(rota);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar rota inexistente")
        void deveLancarExcecaoAoAtualizarRotaInexistente() {
            when(rotaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RotaNotFoundException.class, () -> rotaService.atualizar(99L, new Rota()));

            verify(rotaRepository).findById(99L);
            verify(rotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar com nome duplicado")
        void deveLancarExcecaoAoAtualizarComNomeDuplicado() {
            Rota dados = new Rota();
            dados.setNome("Rota Duplicada");

            when(rotaRepository.findById(1L)).thenReturn(Optional.of(rota));
            when(rotaRepository.countByNomeAndIdNot("Rota Duplicada", 1L)).thenReturn(1L);

            assertThrows(RotaDuplicateException.class, () -> rotaService.atualizar(1L, dados));

            verify(rotaRepository).findById(1L);
            verify(rotaRepository).countByNomeAndIdNot("Rota Duplicada", 1L);
            verify(rotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve manter rota sem alterações quando dados vazios")
        void deveManterRotaSemAlteracoes() {
            Rota dados = new Rota();

            when(rotaRepository.findById(1L)).thenReturn(Optional.of(rota));
            when(rotaRepository.save(any(Rota.class))).thenAnswer(inv -> inv.getArgument(0));

            Rota resultado = rotaService.atualizar(1L, dados);

            assertEquals("Rota Centro", resultado.getNome());
            assertEquals("Cuiabá", resultado.getOrigem());
            assertEquals("Várzea Grande", resultado.getDestino());

            verify(rotaRepository).findById(1L);
            verify(rotaRepository).save(rota);
        }
    }

    @Nested
    class Exclusao {

        @Test
        @DisplayName("Deve deletar rota com sucesso")
        void deveDeletarRota() {
            when(rotaRepository.existsById(1L)).thenReturn(true);

            rotaService.deletar(1L);

            verify(rotaRepository).existsById(1L);
            verify(rotaRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao deletar rota inexistente")
        void deveLancarExcecaoAoDeletarRotaInexistente() {
            when(rotaRepository.existsById(99L)).thenReturn(false);

            assertThrows(RotaNotFoundException.class, () -> rotaService.deletar(99L));

            verify(rotaRepository).existsById(99L);
            verify(rotaRepository, never()).deleteById(any());
        }
    }
}