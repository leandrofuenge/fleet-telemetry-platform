package com.app.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.app.telemetria.domain.entity.Carga;
import com.app.telemetria.domain.entity.Motorista;
import com.app.telemetria.domain.entity.Rota;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.entity.Viagem;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.infrastructure.persistence.ViagemRepository;

@ExtendWith(MockitoExtension.class)
class ViagemServiceTest {

    @Mock
    private ViagemRepository viagemRepository;

    @Mock
    private MotoristaService motoristaService;

    @Mock
    private VeiculoService veiculoService;

    @Mock
    private AlertaService alertaService;

    @InjectMocks
    private ViagemService viagemService;

    private Viagem viagem;
    private Motorista motorista;
    private Veiculo veiculo;
    private Rota rota;
    private Carga carga;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(viagemService, "motoristaService", motoristaService);
        ReflectionTestUtils.setField(viagemService, "veiculoService", veiculoService);
        ReflectionTestUtils.setField(viagemService, "alertaService", alertaService);

        motorista = new Motorista();
        motorista.setId(10L);
        motorista.setNome("Leandro");
        motorista.setCategoriaCnh("D");
        motorista.setDataVencimentoCnh(LocalDate.now().plusDays(30));
        motorista.setDataVencimentoAso(LocalDate.now().plusDays(30));
        motorista.setScore(750);

        veiculo = new Veiculo();
        veiculo.setId(20L);
        veiculo.setPlaca("ABC1D23");
        veiculo.setModelo("Volvo FH");

        rota = new Rota();
        rota.setId(30L);
        rota.setNome("Cuiabá x Rondonópolis");
        rota.setOrigem("Cuiabá");
        rota.setDestino("Rondonópolis");

        carga = new Carga();
        carga.setId(40L);
        carga.setTipo("NORMAL");

        viagem = new Viagem();
        viagem.setId(1L);
        viagem.setMotorista(motorista);
        viagem.setVeiculo(veiculo);
        viagem.setRota(rota);
        viagem.setCarga(carga);
        viagem.setStatus("PLANEJADA");
        viagem.setDataSaida(LocalDateTime.now().plusHours(1));
        viagem.setDataChegadaPrevista(LocalDateTime.now().plusHours(5));
        viagem.setObservacoes("Viagem de teste");
    }

    @Nested
    class Consultas {

        @Test
        @DisplayName("Deve listar todas as viagens")
        void deveListarTodasAsViagens() {
            when(viagemRepository.findAll()).thenReturn(List.of(viagem));

            List<Viagem> resultado = viagemService.listarTodos();

            assertNotNull(resultado);
            assertEquals(1, resultado.size());
            verify(viagemRepository).findAll();
        }

        @Test
        @DisplayName("Deve listar viagens em andamento")
        void deveListarViagensEmAndamento() {
            when(viagemRepository.findByStatus("EM_ANDAMENTO")).thenReturn(List.of(viagem));

            List<Viagem> resultado = viagemService.listarEmAndamento();

            assertNotNull(resultado);
            assertEquals(1, resultado.size());
            verify(viagemRepository).findByStatus("EM_ANDAMENTO");
        }

        @Test
        @DisplayName("Deve buscar viagem por ID")
        void deveBuscarViagemPorId() {
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            Viagem resultado = viagemService.buscarPorId(1L);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            verify(viagemRepository).findById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao buscar viagem inexistente")
        void deveLancarExcecaoAoBuscarViagemInexistente() {
            when(viagemRepository.findById(99L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.buscarPorId(99L)
            );

            assertEquals(ErrorCode.VIAGEM_NOT_FOUND, ex.getErrorCode());
            verify(viagemRepository).findById(99L);
        }

        @Test
        @DisplayName("Deve buscar viagens atrasadas")
        void deveBuscarViagensAtrasadas() {
            when(viagemRepository.findAtrasadas(any(LocalDateTime.class))).thenReturn(List.of(viagem));

            List<Viagem> resultado = viagemService.buscarAtrasadas();

            assertNotNull(resultado);
            assertEquals(1, resultado.size());
            verify(viagemRepository).findAtrasadas(any(LocalDateTime.class));
        }
    }

    @Nested
    class Crud {

        @Test
        @DisplayName("Deve salvar viagem com sucesso")
        void deveSalvarViagemComSucesso() {
            when(viagemRepository.save(any(Viagem.class))).thenReturn(viagem);

            Viagem resultado = viagemService.salvar(viagem);

            assertNotNull(resultado);
            assertEquals("PLANEJADA", resultado.getStatus());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve definir status PLANEJADA se status for nulo ao salvar")
        void deveDefinirStatusPlanejadaSeStatusForNuloAoSalvar() {
            viagem.setStatus(null);
            when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> inv.getArgument(0));

            Viagem resultado = viagemService.salvar(viagem);

            assertEquals("PLANEJADA", resultado.getStatus());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve lançar exceção ao salvar sem veículo")
        void deveLancarExcecaoAoSalvarSemVeiculo() {
            viagem.setVeiculo(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.salvar(viagem)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Veículo é obrigatório"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao salvar sem motorista")
        void deveLancarExcecaoAoSalvarSemMotorista() {
            viagem.setMotorista(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.salvar(viagem)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Motorista é obrigatório"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao salvar sem rota")
        void deveLancarExcecaoAoSalvarSemRota() {
            viagem.setRota(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.salvar(viagem)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Rota é obrigatória"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao salvar sem data de saída")
        void deveLancarExcecaoAoSalvarSemDataSaida() {
            viagem.setDataSaida(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.salvar(viagem)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Data de saída é obrigatória"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve atualizar viagem com sucesso")
        void deveAtualizarViagemComSucesso() {
            Viagem dados = new Viagem();
            dados.setObservacoes("Observação atualizada");
            dados.setStatus("EM_ANDAMENTO");

            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));
            when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> inv.getArgument(0));

            Viagem resultado = viagemService.atualizar(1L, dados);

            assertNotNull(resultado);
            assertEquals("Observação atualizada", resultado.getObservacoes());
            assertEquals("EM_ANDAMENTO", resultado.getStatus());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve definir data de saída ao atualizar status para EM_ANDAMENTO se estiver nula")
        void deveDefinirDataSaidaAoAtualizarStatusParaEmAndamento() {
            viagem.setDataSaida(null);

            Viagem dados = new Viagem();
            dados.setStatus("EM_ANDAMENTO");

            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));
            when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> inv.getArgument(0));

            Viagem resultado = viagemService.atualizar(1L, dados);

            assertEquals("EM_ANDAMENTO", resultado.getStatus());
            assertNotNull(resultado.getDataSaida());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve definir data de chegada real ao atualizar status para FINALIZADA")
        void deveDefinirDataChegadaRealAoAtualizarStatusParaFinalizada() {
            viagem.setDataChegadaReal(null);

            Viagem dados = new Viagem();
            dados.setStatus("FINALIZADA");

            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));
            when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> inv.getArgument(0));

            Viagem resultado = viagemService.atualizar(1L, dados);

            assertEquals("FINALIZADA", resultado.getStatus());
            assertNotNull(resultado.getDataChegadaReal());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve deletar viagem com sucesso")
        void deveDeletarViagemComSucesso() {
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            assertDoesNotThrow(() -> viagemService.deletar(1L));

            verify(viagemRepository).delete(viagem);
        }
    }

    @Nested
    class InicioViagem {

        @Test
        @DisplayName("Deve iniciar viagem com sucesso")
        void deveIniciarViagemComSucesso() {
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));
            when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> viagemService.iniciarViagem(1L));

            assertEquals("EM_ANDAMENTO", viagem.getStatus());
            assertNotNull(viagem.getDataSaida());

            verify(motoristaService).validarCnhParaViagem(motorista);
            verify(motoristaService).validarCategoriaCnhParaVeiculo(motorista, veiculo);
            verify(motoristaService).validarAsoParaViagem(motorista);
            verify(motoristaService).validarMoppingParaCargaPerigosa(motorista, carga);
            verify(motoristaService).validarScoreParaViagem(motorista);
            verify(veiculoService).validarDocumentosParaViagem(veiculo.getId());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve iniciar viagem sem validar MOPP quando carga for nula")
        void deveIniciarViagemSemValidarMoppQuandoCargaForNula() {
            viagem.setCarga(null);

            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));
            when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> viagemService.iniciarViagem(1L));

            verify(motoristaService).validarCnhParaViagem(motorista);
            verify(motoristaService).validarCategoriaCnhParaVeiculo(motorista, veiculo);
            verify(motoristaService).validarAsoParaViagem(motorista);
            verify(motoristaService, never()).validarMoppingParaCargaPerigosa(any(), any());
            verify(motoristaService).validarScoreParaViagem(motorista);
            verify(veiculoService).validarDocumentosParaViagem(veiculo.getId());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve lançar exceção se viagem já estiver em andamento")
        void deveLancarExcecaoSeViagemJaEstiverEmAndamento() {
            viagem.setStatus("EM_ANDAMENTO");
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.iniciarViagem(1L)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("já está em andamento"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção se viagem já estiver finalizada")
        void deveLancarExcecaoSeViagemJaEstiverFinalizada() {
            viagem.setStatus("FINALIZADA");
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.iniciarViagem(1L)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Viagem já foi finalizada"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção se viagem já estiver cancelada")
        void deveLancarExcecaoSeViagemJaEstiverCancelada() {
            viagem.setStatus("CANCELADA");
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.iniciarViagem(1L)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Viagem já foi cancelada"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção se motorista não estiver definido")
        void deveLancarExcecaoSeMotoristaNaoEstiverDefinido() {
            viagem.setMotorista(null);
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.iniciarViagem(1L)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Motorista não definido"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção se veículo não estiver definido")
        void deveLancarExcecaoSeVeiculoNaoEstiverDefinido() {
            viagem.setVeiculo(null);
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.iniciarViagem(1L)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Veículo não definido"));
            verify(viagemRepository, never()).save(any());
        }
    }

    @Nested
    class FinalizacaoECancelamento {

        @Test
        @DisplayName("Deve finalizar viagem com sucesso")
        void deveFinalizarViagemComSucesso() {
            viagem.setStatus("EM_ANDAMENTO");
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));
            when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> viagemService.finalizarViagem(1L));

            assertEquals("FINALIZADA", viagem.getStatus());
            assertNotNull(viagem.getDataChegadaReal());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve lançar exceção ao finalizar viagem que não está em andamento")
        void deveLancarExcecaoAoFinalizarViagemQueNaoEstaEmAndamento() {
            viagem.setStatus("PLANEJADA");
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.finalizarViagem(1L)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("Apenas viagens em andamento"));
            verify(viagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve cancelar viagem com sucesso")
        void deveCancelarViagemComSucesso() {
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));
            when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> viagemService.cancelarViagem(1L, "Problema mecânico"));

            assertEquals("CANCELADA", viagem.getStatus());
            assertEquals("CANCELADA: Problema mecânico", viagem.getObservacoes());
            verify(viagemRepository).save(viagem);
        }

        @Test
        @DisplayName("Deve lançar exceção ao cancelar viagem já finalizada")
        void deveLancarExcecaoAoCancelarViagemJaFinalizada() {
            viagem.setStatus("FINALIZADA");
            when(viagemRepository.findById(1L)).thenReturn(Optional.of(viagem));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> viagemService.cancelarViagem(1L, "Motivo qualquer")
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("já finalizada"));
            verify(viagemRepository, never()).save(any());
        }
    }
}